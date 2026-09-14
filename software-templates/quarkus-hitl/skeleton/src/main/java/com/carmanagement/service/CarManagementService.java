package com.carmanagement.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.carmanagement.agentic.BookValue;
import com.carmanagement.agentic.FeedbackVerdict;
import com.carmanagement.agentic.agents.CleaningAgent;
import com.carmanagement.agentic.agents.DispositionProposalAgent;
import com.carmanagement.agentic.agents.MaintenanceAgent;
import com.carmanagement.agentic.agents.PricingAgent;
import com.carmanagement.agentic.trace.WorkflowResult;
import com.carmanagement.agentic.trace.WorkflowTrace;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.agentic.workflow.FeedbackWorkflow;
import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import com.carmanagement.model.FeedbackAnalysisResults;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class CarManagementService {

    private static final String TYPE = "supervisor";
    private final ConcurrentHashMap<Integer, Boolean> inflight = new ConcurrentHashMap<>();

    @Inject
    FeedbackWorkflow feedbackWorkflow;

    @Inject
    PricingAgent pricingAgent;

    @Inject
    DispositionProposalAgent dispositionProposalAgent;

    @Inject
    MaintenanceAgent maintenanceAgent;

    @Inject
    CleaningAgent cleaningAgent;

    @Inject
    ApprovalService approvalService;

    /**
     * Run the agent workflow on a worker that is not tied to the HTTP request.
     * Dev Spaces / Che aborts long POSTs; HITL can wait up to 5 minutes.
     */
    public WorkflowResult startCarReturn(Integer carNumber, String feedback) {
        CarInfo carInfo = findCarInfo(carNumber);
        if (carInfo == null) {
            throw new IllegalArgumentException("Car not found with number: " + carNumber);
        }
        if (inflight.putIfAbsent(carNumber, Boolean.TRUE) != null) {
            return WorkflowTrace.processing(carInfo);
        }
        Infrastructure.getDefaultWorkerPool().execute(() -> {
            try {
                run(carNumber, feedback != null ? feedback : "");
            } catch (Exception e) {
                Log.errorf(e, "Car return failed for #%d", carNumber);
            } finally {
                inflight.remove(carNumber);
            }
        });
        return WorkflowTrace.processing(carInfo);
    }

    public boolean isInFlight(Integer carNumber) {
        return inflight.containsKey(carNumber);
    }

    private WorkflowResult run(Integer carNumber, String feedback) {
        CarInfo carInfo = findCarInfo(carNumber);
        if (carInfo == null) {
            throw new IllegalArgumentException("Car not found with number: " + carNumber);
        }

        WorkflowTrace.start(TYPE, carNumber, feedback);
        FeedbackAnalysisResults analysis = analyze(carInfo, carNumber, feedback);
        WorkflowTrace.agent("CleaningFeedback", analysis.cleaningAnalysis());
        WorkflowTrace.agent("MaintenanceFeedback", analysis.maintenanceAnalysis());
        WorkflowTrace.agent("DispositionFeedback", analysis.dispositionAnalysis());
        CarConditions carConditions;
        if (FeedbackVerdict.dispositionRequired(analysis, feedback)) {
            carConditions = runHitl(carInfo, carNumber, feedback, analysis);
        } else {
            carConditions = runRoutine(carInfo, carNumber, feedback, analysis);
        }
        Map<String, Object> intake = carConditions.intake() != null ? carConditions.intake() : Map.of();
        WorkflowTrace.assignment(TYPE, intake);

        String action = WorkflowTrace.statusFor(intake);
        persistOutcome(carNumber, action, carConditions.generalCondition());
        carInfo = findCarInfo(carNumber);

        WorkflowTrace.applyTools(carNumber, WorkflowTrace.snapshot(carInfo), intake);
        WorkflowTrace.done(TYPE, carNumber, action);
        return WorkflowTrace.response(carInfo, TYPE, intake);
    }

    private FeedbackAnalysisResults analyze(CarInfo carInfo, Integer carNumber, String feedback) {
        try {
            return feedbackWorkflow.analyzeFeedback(carInfo, carNumber, feedback);
        } catch (Exception e) {
            Log.warnf(e, "Feedback agents failed for car #%d; using keyword routing", carNumber);
            return new FeedbackAnalysisResults(
                    "CLEANING_NOT_REQUIRED",
                    "MAINTENANCE_NOT_REQUIRED",
                    FeedbackVerdict.looksLikeWriteOff(feedback)
                            ? "DISPOSITION_REQUIRED: " + feedback
                            : "DISPOSITION_NOT_REQUIRED");
        }
    }

    /**
     * Specialists (pricing, proposal) stay on the LLM. Java only enforces the
     * policy the supervisor kept skipping: a write-off must pause for a human.
     */
    private CarConditions runHitl(CarInfo carInfo, Integer carNumber, String feedback,
            FeedbackAnalysisResults analysis) {
        String originalCondition = carInfo.condition;
        String liveCondition = FeedbackVerdict.applyLiveCondition(carInfo, feedback, analysis);
        String carValue = BookValue.estimate(carInfo);
        String proposal = "";
        try {
            carValue = pricingAgent.estimateValue(
                    carInfo.make,
                    carInfo.model,
                    carInfo.year,
                    BookValue.currentYear(),
                    BookValue.age(carInfo.year),
                    originalCondition);
        } catch (Exception e) {
            Log.warnf(e, "PricingAgent failed for car #%d; using book value %s", carNumber, carValue);
        }
        WorkflowTrace.agent("PricingAgent", carValue);
        try {
            proposal = dispositionProposalAgent.createDispositionProposal(
                    carInfo.make, carInfo.model, carInfo.year, carNumber,
                    liveCondition, carValue, feedback);
        } catch (Exception e) {
            Log.warnf(e, "DispositionProposalAgent failed for car #%d", carNumber);
        }
        WorkflowTrace.agent("DispositionProposalAgent", proposal);
        String proposed = FeedbackVerdict.dispositionActionOrScrap(proposal, analysis.dispositionAnalysis());
        String reason = (proposal != null && !proposal.isBlank())
                ? proposal
                : "Recommended " + proposed + " at " + carValue + ". " + feedback;
        WorkflowTrace.agent("HumanApprovalAgent", "waiting for reviewer car=#" + carNumber
                + " proposed=" + proposed + " value=" + carValue);
        WorkflowTrace.dispositionTool(carNumber, proposed, money(carValue));

        CompletableFuture<ApprovalProposal> future = approvalService.createProposalAndWaitForDecision(
                carNumber,
                carInfo.make,
                carInfo.model,
                carInfo.year,
                carValue,
                proposed,
                reason,
                liveCondition,
                feedback);

        String supervisorDecision;
        try {
            ApprovalProposal result = future.get(5, TimeUnit.MINUTES);
            supervisorDecision = decisionToken(result);
            WorkflowTrace.agent("HumanApprovalAgent", supervisorDecision
                    + (result.approvalReason != null ? " " + result.approvalReason : ""));
        } catch (TimeoutException e) {
            Log.error("HITL timeout: no human decision within 5 minutes, defaulting to KEEP_CAR");
            supervisorDecision = "KEEP_CAR";
        } catch (Exception e) {
            Log.errorf(e, "HITL error while waiting for human approval");
            supervisorDecision = "KEEP_CAR";
        }

        FeedbackAnalysisResults forOutput = analysis;
        if ("KEEP_CAR".equals(supervisorDecision)) {
            forOutput = new FeedbackAnalysisResults(
                    analysis.cleaningAnalysis(),
                    "MAINTENANCE_REQUIRED: repair after collision",
                    "DISPOSITION_NOT_REQUIRED");
        }
        String condition = "KEEP_CAR".equals(supervisorDecision)
                ? "Keep and repair after collision"
                : "SCRAP - severe damage, repair cost exceeds value";
        CarConditions result = CarProcessingWorkflow.output(
                forOutput,
                supervisorDecision + " " + carValue,
                new CarConditions(condition, com.carmanagement.model.CarAssignment.NONE));
        if (result.intake() != null) {
            result.intake().put("carValue", money(carValue));
            result.intake().put("dispositionAction", proposed);
        }
        return result;
    }

    private CarConditions runRoutine(CarInfo carInfo, Integer carNumber, String feedback,
            FeedbackAnalysisResults analysis) {
        String supervisorDecision = "KEEP_CAR";
        try {
            if (FeedbackVerdict.required(analysis.maintenanceAnalysis(), "MAINTENANCE_NOT_REQUIRED")) {
                String plan = maintenanceAgent.processMaintenance(
                        carInfo.make, carInfo.model, carInfo.year, carNumber, analysis.maintenanceAnalysis());
                WorkflowTrace.agent("MaintenanceAgent", plan);
            } else if (FeedbackVerdict.required(analysis.cleaningAnalysis(), "CLEANING_NOT_REQUIRED")) {
                String plan = cleaningAgent.processCleaning(
                        carInfo.make, carInfo.model, carInfo.year, carNumber, analysis.cleaningAnalysis());
                WorkflowTrace.agent("CleaningAgent", plan);
            }
        } catch (Exception e) {
            Log.warnf(e, "Routine agents failed for car #%d", carNumber);
        }
        String condition = carInfo.condition != null ? carInfo.condition : feedback;
        return CarProcessingWorkflow.output(
                analysis,
                supervisorDecision,
                new CarConditions(condition, com.carmanagement.model.CarAssignment.NONE));
    }

    private static String decisionToken(ApprovalProposal result) {
        if (result == null) {
            return "KEEP_CAR";
        }
        String blob = ((result.approvalReason == null ? "" : result.approvalReason) + " "
                + (result.decision == null ? "" : result.decision)).toUpperCase();
        if (blob.contains("KEEP_CAR")) {
            return "KEEP_CAR";
        }
        if (blob.contains("DISPOSE_CAR")) {
            return "DISPOSE_CAR";
        }
        if ("REJECTED".equalsIgnoreCase(result.decision)) {
            return "KEEP_CAR";
        }
        return "DISPOSE_CAR";
    }

    private static String money(String carValue) {
        if (carValue == null || carValue.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$[0-9,]+").matcher(carValue);
        return matcher.find() ? matcher.group() : carValue;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    CarInfo findCarInfo(Integer carNumber) {
        return CarInfo.findById(carNumber);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void persistOutcome(Integer carNumber, String action, String condition) {
        CarInfo carInfo = CarInfo.findById(carNumber);
        carInfo.condition = condition;
        carInfo.status = CarStatus.valueOf(action);
        CarInfo.getEntityManager().merge(carInfo);
    }
}
