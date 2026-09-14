package com.carmanagement.agentic.agents;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.spi.CDI;

import com.carmanagement.agentic.FeedbackVerdict;
import com.carmanagement.agentic.hitl.HitlContext;
import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.model.CarInfo;
import com.carmanagement.service.ApprovalService;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.HumanInTheLoop;
import dev.langchain4j.agentic.declarative.HumanInTheLoopResponseSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.scope.AgenticScope;
import io.quarkus.logging.Log;

/**
 * Pauses the supervisor until a human approves or rejects a high-value disposition.
 * <p>
 * LangChain4j 1.11 {@code @HumanInTheLoop} must be a static void method with exactly one
 * parameter; the matching {@code @HumanInTheLoopResponseSupplier} returns the decision
 * string so {@code SupervisorPlanner} does not NPE on a null agent output. {@code @Agent}
 * is also required so quarkus-langchain4j-agentic 1.7.6 indexes this class as a supervisor
 * sub-agent (HITL alone is not in {@code ALL_AGENT_ANNOTATIONS}).
 */
public class HumanApprovalAgent {

    private static final Pattern ACTION = Pattern.compile("__(SCRAP|SELL|DONATE|KEEP)__");
    private static final ThreadLocal<AgenticScope> CURRENT_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<CompletableFuture<ApprovalProposal>> PENDING = new ThreadLocal<>();

    @AgentListenerSupplier
    static AgentListener listener() {
        return new AgentListener() {
            @Override
            public boolean inheritedBySubagents() {
                return true;
            }

            @Override
            public void beforeAgentInvocation(AgentRequest request) {
                CURRENT_SCOPE.set(request.agenticScope());
                HitlContext.capture(request.agenticScope(), request.agentName(), null);
            }

            @Override
            public void afterAgentInvocation(dev.langchain4j.agentic.observability.AgentResponse response) {
                HitlContext.capture(response.agenticScope(), response.agentName(), response.output());
            }
        };
    }

    @Agent(outputKey = "approvalDecision",
            description = "Coordinates human approval for high-value vehicle dispositions")
    @HumanInTheLoop(description = "Coordinates human approval for high-value vehicle dispositions",
            outputKey = "approvalDecision")
    public static void requestApproval(CarInfo carInfo) {
        AgenticScope scope = CURRENT_SCOPE.get();
        Integer carNumber = carInfo != null && carInfo.id != null
                ? carInfo.id.intValue()
                : scopeInt(scope, "carNumber");
        HitlContext.Snapshot snap = HitlContext.snapshot(carNumber, scope, carInfo);
        String carMake = carInfo != null ? carInfo.make : "";
        String carModel = carInfo != null ? carInfo.model : "";
        Integer carYear = carInfo != null ? carInfo.year : 0;
        String carCondition = HitlContext.firstNonBlank(snap.carCondition(),
                carInfo != null ? carInfo.condition : "");
        String feedback = HitlContext.firstNonBlank(snap.feedback(), scopeString(scope, "feedback"), carCondition);
        String carValue = HitlContext.firstNonBlank(snap.carValue(), scopeString(scope, "carValue"));
        String dispositionProposal = HitlContext.firstNonBlank(
                snap.dispositionProposal(), scopeString(scope, "dispositionProposal"));
        String proposed = extractAction(dispositionProposal);
        if (proposed.isBlank() || "UNKNOWN".equals(proposed) || proposed.length() > 16) {
            proposed = FeedbackVerdict.dispositionActionOrScrap(dispositionProposal, feedback + " " + carCondition);
        }
        String reason = HitlContext.firstNonBlank(dispositionProposal,
                formatFallbackReason(proposed, carValue, carCondition, feedback));

        Log.debugf("HITL: creating approval proposal for car %d — %s %s %s (value=%s proposed=%s feedback=%s)",
                carNumber, carYear, carMake, carModel, carValue, proposed,
                feedback.length() > 80 ? feedback.substring(0, 80) + "…" : feedback);

        ApprovalService approvalService = CDI.current().select(ApprovalService.class).get();
        PENDING.set(approvalService.createProposalAndWaitForDecision(
                carNumber, carMake, carModel, carYear, carValue,
                proposed, reason, carCondition, feedback));
    }

    @HumanInTheLoopResponseSupplier
    public static String waitForResponse() {
        try {
            CompletableFuture<ApprovalProposal> future = PENDING.get();
            if (future == null) {
                return """
                    Human Decision: REJECTED
                    Reason: No pending approval future
                    Approved By: System (Error)
                    """;
            }
            ApprovalProposal result = future.get(5, TimeUnit.MINUTES);
            HitlContext.clear(result.carNumber);
            Log.debugf("HITL resumed — human decision: %s", result.decision);
            return String.format("""
                Human Decision: %s
                Reason: %s
                Approved By: %s
                Decision Time: %s
                """,
                    result.decision,
                    result.approvalReason != null ? result.approvalReason : "No reason provided",
                    result.approvedBy != null ? result.approvedBy : "Unknown",
                    result.decidedAt != null ? result.decidedAt.toString() : "Unknown");
        } catch (TimeoutException e) {
            Log.error("HITL timeout: no human decision within 5 minutes, defaulting to REJECTED");
            return """
                Human Decision: REJECTED
                Reason: Timeout - No human decision received within 5 minutes. Defaulting to rejection for safety.
                Approved By: System (Timeout)
                """;
        } catch (Exception e) {
            Log.errorf(e, "HITL error while waiting for human approval");
            return String.format("""
                Human Decision: REJECTED
                Reason: Error occurred while waiting for human approval: %s
                Approved By: System (Error)
                """, e.getMessage());
        } finally {
            PENDING.remove();
            CURRENT_SCOPE.remove();
        }
    }

    private static Integer scopeInt(AgenticScope scope, String key) {
        if (scope == null) {
            return 0;
        }
        Object value = scope.readState(key);
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static String scopeString(AgenticScope scope, String key) {
        if (scope == null) {
            return "";
        }
        Object value = scope.readState(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String extractAction(String proposal) {
        if (proposal == null || proposal.isBlank()) {
            return "";
        }
        Matcher matcher = ACTION.matcher(proposal.toUpperCase());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return FeedbackVerdict.extractDispositionAction(proposal);
    }

    private static String formatFallbackReason(String action, String carValue, String carCondition, String feedback) {
        StringBuilder reason = new StringBuilder();
        reason.append("Recommended ").append(action == null || action.isBlank() ? "SCRAP" : action);
        reason.append(" because this is a high-value vehicle");
        if (carValue != null && !carValue.isBlank()) {
            reason.append(" (").append(carValue.replaceAll("\\s+", " ").trim()).append(")");
        }
        reason.append(" with severe damage");
        if (carCondition != null && !carCondition.isBlank()) {
            reason.append(": ").append(carCondition);
        } else if (feedback != null && !feedback.isBlank()) {
            reason.append(": ").append(feedback);
        }
        reason.append(". Human review is required before removing it from the fleet.");
        return reason.toString();
    }
}
