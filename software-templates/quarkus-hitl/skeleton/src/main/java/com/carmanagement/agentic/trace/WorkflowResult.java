package com.carmanagement.agentic.trace;

import java.util.Map;

/**
 * Same envelope as Camel {@code workflowSupport.response}: {@code { car, workflow }}.
 */
public record WorkflowResult(
        Map<String, Object> car,
        Map<String, Object> workflow
) {
}
