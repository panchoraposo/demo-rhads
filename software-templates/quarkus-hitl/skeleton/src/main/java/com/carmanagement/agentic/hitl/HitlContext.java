package com.carmanagement.agentic.hitl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * The HITL method can only take {@link CarInfo}, so pricing, feedback, and the
 * disposition proposal are snapshotted from the shared {@link AgenticScope}
 * as earlier agents run.
 */
public final class HitlContext {

    public record Snapshot(
            String feedback,
            String carValue,
            String dispositionProposal,
            String carCondition) {
        static final Snapshot EMPTY = new Snapshot("", "", "", "");
    }

    private static final ConcurrentHashMap<Integer, Snapshot> BY_CAR = new ConcurrentHashMap<>();

    private HitlContext() {
    }

    public static void capture(AgenticScope scope, String agentName, Object output) {
        if (scope == null) {
            return;
        }
        int carNumber = carNumber(scope);
        if (carNumber == 0) {
            return;
        }
        Snapshot prev = BY_CAR.getOrDefault(carNumber, Snapshot.EMPTY);
        Map<String, Object> state = scope.state();
        String feedback = firstNonBlank(read(state, "feedback"), prev.feedback());
        String carValue = firstNonBlank(read(state, "carValue"), prev.carValue());
        String proposal = firstNonBlank(read(state, "dispositionProposal"), prev.dispositionProposal());
        String condition = firstNonBlank(read(state, "carCondition"), prev.carCondition());
        Object carInfo = state.get("carInfo");
        if (carInfo instanceof CarInfo info && info.condition != null && !info.condition.isBlank()) {
            condition = firstNonBlank(condition, info.condition);
        }
        if (output != null) {
            String name = agentName == null ? "" : agentName;
            if (isPricing(name)) {
                carValue = firstNonBlank(String.valueOf(output), carValue);
            }
            if (isProposal(name)) {
                proposal = firstNonBlank(String.valueOf(output), proposal);
            }
        }
        if (scope.agentInvocations() != null) {
            for (AgentInvocation invocation : scope.agentInvocations()) {
                String name = invocation.agentName() == null ? "" : invocation.agentName();
                String type = invocation.agentType() != null ? invocation.agentType().getSimpleName() : "";
                Object value = invocation.output();
                if (value == null) {
                    continue;
                }
                if (isPricing(name) || isPricing(type)) {
                    carValue = firstNonBlank(String.valueOf(value), carValue);
                }
                if (isProposal(name) || isProposal(type)) {
                    proposal = firstNonBlank(String.valueOf(value), proposal);
                }
            }
        }
        BY_CAR.put(carNumber, new Snapshot(feedback, carValue, proposal, condition));
    }

    public static Snapshot snapshot(Integer carNumber, AgenticScope scope, CarInfo carInfo) {
        if (scope != null) {
            capture(scope, null, null);
        }
        int id = carNumber != null ? carNumber : 0;
        if (id == 0 && carInfo != null && carInfo.id != null) {
            id = carInfo.id.intValue();
        }
        Snapshot cached = BY_CAR.getOrDefault(id, Snapshot.EMPTY);
        String condition = cached.carCondition();
        if ((condition == null || condition.isBlank()) && carInfo != null) {
            condition = carInfo.condition;
        }
        return new Snapshot(cached.feedback(), cached.carValue(), cached.dispositionProposal(), condition);
    }

    public static void clear(Integer carNumber) {
        if (carNumber != null) {
            BY_CAR.remove(carNumber);
        }
    }

    private static int carNumber(AgenticScope scope) {
        Object value = scope.readState("carNumber");
        if (value instanceof Number number) {
            return number.intValue();
        }
        Object carInfo = scope.readState("carInfo");
        if (carInfo instanceof CarInfo info && info.id != null) {
            return info.id.intValue();
        }
        return 0;
    }

    private static String read(Map<String, Object> state, String key) {
        if (state == null) {
            return "";
        }
        Object value = state.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean isPricing(String name) {
        String text = name == null ? "" : name.toLowerCase();
        return text.contains("pricing") || text.contains("estimatevalue");
    }

    private static boolean isProposal(String name) {
        String text = name == null ? "" : name.toLowerCase();
        return text.contains("proposal") || text.contains("createdisposition");
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return "";
    }
}
