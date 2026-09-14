package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.FeedbackVerdict;
import com.carmanagement.agentic.agents.CarConditionFeedbackAgent;
import com.carmanagement.agentic.agents.FleetSupervisorAgent;
import com.carmanagement.agentic.trace.WorkflowTrace;
import com.carmanagement.agentic.trace.WorkflowTraceListener;
import com.carmanagement.model.CarAssignment;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Workflow for processing car returns using a supervisor agent for complete orchestration.
 * The supervisor coordinates action agents based on parallel feedback analysis.
 */
public interface CarProcessingWorkflow {

    @SequenceAgent(outputKey = "carProcessingAgentResult",
            subAgents = { FeedbackWorkflow.class, FleetSupervisorAgent.class, CarConditionFeedbackAgent.class })
    CarConditions processCarReturn(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);

    @AgentListenerSupplier
    static AgentListener workflowListener() {
        return new WorkflowTraceListener();
    }

    @Output
    static CarConditions output(
            FeedbackAnalysisResults feedbackAnalysisResults,
            String supervisorDecision,
            CarConditions carConditions) {
        CarAssignment assignment = FeedbackVerdict.assignment(feedbackAnalysisResults, supervisorDecision);
        String action = FeedbackVerdict.dispositionActionOrScrap(
                supervisorDecision, feedbackAnalysisResults.dispositionAnalysis());
        String condition = carConditions != null ? carConditions.generalCondition() : "";
        if (assignment == CarAssignment.DISPOSITION) {
            condition = FeedbackVerdict.formatDispositionCondition(action, condition);
        }
        String dispositionStatus = assignment == CarAssignment.DISPOSITION
                ? "DISPOSITION_APPROVED"
                : "DISPOSITION_NOT_REQUIRED";
        java.util.Map<String, Object> intake = WorkflowTrace.intake(
                feedbackAnalysisResults.cleaningAnalysis(),
                feedbackAnalysisResults.maintenanceAnalysis(),
                feedbackAnalysisResults.dispositionAnalysis(),
                condition,
                supervisorDecision);
        if (assignment != CarAssignment.DISPOSITION) {
            intake.put("dispositionRequired", false);
        } else {
            intake.put("dispositionAction", action);
            intake.put("condition", condition);
        }
        return new CarConditions(
                condition,
                assignment,
                dispositionStatus,
                FeedbackVerdict.withoutThinking(supervisorDecision),
                intake);
    }
}
