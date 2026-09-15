package com.carmanagement.agentic.trace;

import java.util.Map;
import java.util.Set;

import com.carmanagement.agentic.hitl.HitlContext;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;

/**
 * Emits Camel-style {@code [ai] AgentName raw=…} lines for specialist/intake agents.
 */
public final class WorkflowTraceListener implements AgentListener {

    private static final Set<String> SKIP = Set.of(
            "CarProcessingWorkflow",
            "FeedbackWorkflow",
            "CarAssignmentWorkflow",
            "ImageAnalysisWorkflow",
            "FleetSupervisorAgent",
            "CarConditionFeedbackAgent",
            "DispositionAgent",
            "DispositionProposalAgent",
            "HumanApprovalAgent",
            "MaintenanceAgent",
            "ImageAnalysisAgent"
    );

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    @Override
    public void beforeAgentInvocation(AgentRequest request) {
        HitlContext.capture(request.agenticScope(), request.agentName(), null);
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        HitlContext.capture(agentResponse.agenticScope(), agentResponse.agentName(), agentResponse.output());
        String name = agentResponse.agentName();
        if ((name == null || name.isBlank()) && agentResponse.agent() != null && agentResponse.agent().type() != null) {
            name = agentResponse.agent().type().getSimpleName();
        }
        if (agentResponse.agent() != null && agentResponse.agent().type() != null) {
            String typeName = agentResponse.agent().type().getSimpleName();
            if (SKIP.contains(typeName)) {
                return;
            }
            name = typeName;
        }
        if (name == null || SKIP.contains(name)) {
            return;
        }
        Map<String, Object> inputs = agentResponse.inputs();
        if ("CleaningAgent".equals(name)) {
            if (inputs != null && inputs.containsKey("cleaningRequest")) {
                return;
            }
            WorkflowTrace.agent("IntakeAgent", agentResponse.output());
            return;
        }
        if (name.endsWith("FeedbackAgent")) {
            WorkflowTrace.agent(name.replace("Agent", ""), agentResponse.output());
            return;
        }
        if ("CleaningFeedback".equals(name) || "MaintenanceFeedback".equals(name) || "DispositionFeedback".equals(name)) {
            WorkflowTrace.agent(name, agentResponse.output());
        }
    }
}
