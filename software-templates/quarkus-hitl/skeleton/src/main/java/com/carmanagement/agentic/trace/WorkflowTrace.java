package com.carmanagement.agentic.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.carmanagement.agentic.FeedbackVerdict;
import com.carmanagement.model.CarInfo;
import io.quarkus.logging.Log;

/**
 * Shared workflow logs and {@code {car, workflow}} JSON envelope.
 * Same intake flags and status priority as {@code demo-camel-agentic};
 * supervisor lines name LangChain4j agents ({@code agent=MaintenanceAgent}),
 * not Camel {@code direct:} routes.
 */
public final class WorkflowTrace {

    private WorkflowTrace() {
    }

    public static void start(String type, Integer carNumber, String feedback) {
        Log.info("▶ " + type + " car=#" + carNumber + " feedback=" + (feedback == null ? "" : feedback));
    }

    public static void agent(String name, Object output) {
        Log.info("🧠 " + name + " raw=" + compact(output));
    }

    /** 202 body while the worker runs (Che cannot hold a 5-minute HITL POST). Same envelope as the finished 200. */
    public static WorkflowResult processing(CarInfo carInfo) {
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("type", "supervisor");
        workflow.put("action", "PROCESSING");
        workflow.put("intake", Map.of());
        workflow.put("steps", List.of("intake"));
        return new WorkflowResult(snapshot(carInfo), workflow);
    }

    public static void assignment(String type, Map<String, Object> intake) {
        boolean cleaning = flag(intake, "cleaningRequired");
        boolean maintenance = flag(intake, "maintenanceRequired");
        boolean disposition = flag(intake, "dispositionRequired");
        String assignment = assignmentName(intake);
        switch (type) {
            case "supervisor" -> {
                String agent = switch (assignment) {
                    case "DISPOSITION" -> "PricingAgent+DispositionProposalAgent+HumanApprovalAgent";
                    case "MAINTENANCE" -> "MaintenanceAgent";
                    case "CLEANING" -> "CleaningAgent";
                    default -> "(none)";
                };
                Log.info("🧠 supervisor assignment=" + assignment + " agent=" + agent);
                if ("DISPOSITION".equals(assignment)) {
                    Log.info("Supervisor → PricingAgent + DispositionProposalAgent + HumanApprovalAgent");
                } else if ("NONE".equals(assignment)) {
                    Log.info("Supervisor: car remains available");
                }
            }
            case "conditional" -> {
                Log.info("🧠 flags cleaning=" + cleaning + " maintenance=" + maintenance);
                if (!maintenance && !cleaning) {
                    Log.info("Conditional: car remains available");
                }
            }
            default -> {
                Log.info("🧠 cleaning=" + cleaning + " maintenance=" + maintenance
                        + " condition=" + String.valueOf(intake.getOrDefault("condition", "")));
                if ("agent".equals(type) && !cleaning) {
                    Log.info("CLEANING_NOT_REQUIRED");
                }
            }
        }
        if (disposition && !"supervisor".equals(type)) {
            Log.info("🧠 disposition=" + disposition);
        }
    }

    public static void applyTools(Integer carNumber, Map<String, Object> car, Map<String, Object> intake) {
        String action = statusFor(intake);
        switch (action) {
            case "PENDING_DISPOSITION" -> {
                WorkOrderWriter.disposition(carNumber, car, intake);
                dispositionTool(carNumber, intake.getOrDefault("dispositionAction", ""),
                        intake.getOrDefault("carValue", ""));
            }
            case "AT_MAINTENANCE" -> {
                WorkOrderWriter.maintenance(carNumber, car, intake);
                Log.info("🔧 MaintenanceTool car=#" + carNumber);
            }
            case "AT_CLEANING" -> {
                WorkOrderWriter.cleaning(carNumber, car, intake);
                Log.info("🚗 CleaningTool result: " + cleaningToolResult(carNumber, car, intake));
            }
            default -> {
            }
        }
    }

    public static void dispositionTool(Integer carNumber, Object action, Object value) {
        Log.info("📋 DispositionTool car=#" + carNumber + " action=" + action + " value=" + value);
    }

    public static void done(String type, Integer carNumber, String action) {
        Log.info("✅ " + type + " car=#" + carNumber + " action=" + action);
    }

    public static WorkflowResult response(CarInfo carInfo, String type, Map<String, Object> intake) {
        Map<String, Object> car = snapshot(carInfo);
        String action = statusFor(intake);
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("type", type);
        workflow.put("action", action);
        workflow.put("intake", intake);
        workflow.put("steps", inferredSteps(type, intake));
        return new WorkflowResult(car, workflow);
    }

    public static Map<String, Object> snapshot(CarInfo carInfo) {
        Map<String, Object> car = new LinkedHashMap<>();
        car.put("id", carInfo.id);
        car.put("make", carInfo.make);
        car.put("model", carInfo.model);
        car.put("year", carInfo.year);
        car.put("status", carInfo.status != null ? carInfo.status.name() : null);
        Object condition = "";
        try {
            condition = carInfo.getClass().getField("condition").get(carInfo);
        } catch (ReflectiveOperationException ignored) {
            // step-01 has no condition column
        }
        car.put("condition", condition == null ? "" : condition);
        return car;
    }

    public static Map<String, Object> intake(
            String cleaningAnalysis,
            String maintenanceAnalysis,
            String dispositionAnalysis,
            String condition,
            String supervisorDecision) {
        Map<String, Object> intake = new LinkedHashMap<>();
        boolean cleaning = FeedbackVerdict.required(cleaningAnalysis, "CLEANING_NOT_REQUIRED");
        boolean maintenance = FeedbackVerdict.required(maintenanceAnalysis, "MAINTENANCE_NOT_REQUIRED");
        boolean disposition = FeedbackVerdict.required(dispositionAnalysis, "DISPOSITION_NOT_REQUIRED");
        intake.put("cleaningRequired", cleaning);
        intake.put("maintenanceRequired", maintenance);
        intake.put("dispositionRequired", disposition);
        intake.put("cleaningServices", List.of());
        intake.put("maintenanceServices", List.of());
        intake.put("cleaningNotes", notes(cleaningAnalysis, cleaning, "CLEANING_NOT_REQUIRED"));
        intake.put("maintenanceNotes", notes(maintenanceAnalysis, maintenance, "MAINTENANCE_NOT_REQUIRED"));
        intake.put("dispositionNotes", notes(dispositionAnalysis, disposition, "DISPOSITION_NOT_REQUIRED"));
        intake.put("dispositionAction", disposition ? dispositionAction(supervisorDecision, dispositionAnalysis) : "");
        intake.put("carValue", extractValue(supervisorDecision));
        intake.put("condition", condition == null ? "" : FeedbackVerdict.withoutThinking(condition));
        return intake;
    }

    public static String statusFor(Map<String, Object> intake) {
        if (flag(intake, "dispositionRequired")) {
            return "PENDING_DISPOSITION";
        }
        if (flag(intake, "maintenanceRequired")) {
            return "AT_MAINTENANCE";
        }
        if (flag(intake, "cleaningRequired")) {
            return "AT_CLEANING";
        }
        return "AVAILABLE";
    }

    public static String assignmentName(Map<String, Object> intake) {
        if (flag(intake, "dispositionRequired")) {
            return "DISPOSITION";
        }
        if (flag(intake, "maintenanceRequired")) {
            return "MAINTENANCE";
        }
        if (flag(intake, "cleaningRequired")) {
            return "CLEANING";
        }
        return "NONE";
    }

    public static boolean flag(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static List<String> inferredSteps(String type, Map<String, Object> intake) {
        List<String> steps = new ArrayList<>();
        steps.add("intake");
        boolean cleaning = flag(intake, "cleaningRequired");
        boolean maintenance = flag(intake, "maintenanceRequired");
        boolean disposition = flag(intake, "dispositionRequired");
        switch (type) {
            case "supervisor" -> {
                if (disposition) {
                    steps.add("pricing-agent");
                    steps.add("disposition-tool");
                } else if (maintenance) {
                    steps.add("maintenance-tool");
                } else if (cleaning) {
                    steps.add("cleaning-tool");
                } else {
                    steps.add("available");
                }
            }
            case "parallel" -> {
                if (cleaning) {
                    steps.add("cleaning-tool");
                }
                if (maintenance) {
                    steps.add("maintenance-tool");
                }
            }
            case "conditional" -> {
                if (maintenance) {
                    steps.add("maintenance-tool");
                } else if (cleaning) {
                    steps.add("cleaning-tool");
                } else {
                    steps.add("available");
                }
            }
            default -> {
                if (cleaning) {
                    steps.add("cleaning-tool");
                }
            }
        }
        steps.add("update");
        return steps;
    }


    /**
     * Same text the workshop shows for {@code CleaningTool} (section-2/step-01).
     */
    public static String cleaningToolResult(Integer carNumber, Map<String, Object> car, Map<String, Object> intake) {
        if (car == null) {
            car = Map.of();
        }
        if (intake == null) {
            intake = Map.of();
        }
        int year = 0;
        Object rawYear = car.get("year");
        if (rawYear instanceof Number number) {
            year = number.intValue();
        }
        int age = Math.max(0, java.time.Year.now().getValue() - year);
        StringBuilder summary = new StringBuilder();
        summary.append("Cleaning requested for ").append(age).append("-year-old ")
                .append(car.getOrDefault("make", "")).append(" ")
                .append(car.getOrDefault("model", ""))
                .append(", Car #").append(carNumber).append(":\n");
        boolean any = false;
        Object services = intake.get("cleaningServices");
        if (services instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    summary.append("- ").append(item).append("\n");
                    any = true;
                }
            }
        }
        if (!any) {
            summary.append("- Interior cleaning\n");
        }
        Object notes = intake.get("cleaningNotes");
        if (notes != null && !String.valueOf(notes).isBlank()
                && !String.valueOf(notes).toUpperCase().contains("CLEANING_NOT_REQUIRED")) {
            summary.append("Additional notes: ").append(notes);
        }
        return summary.toString();
    }

    private static String notes(String analysis, boolean required, String notRequiredToken) {
        String text = FeedbackVerdict.withoutThinking(analysis);
        if (text.isBlank()) {
            return required ? "" : notRequiredToken;
        }
        return text;
    }

    private static String dispositionAction(String supervisorDecision, String dispositionAnalysis) {
        return FeedbackVerdict.dispositionActionOrScrap(supervisorDecision, dispositionAnalysis);
    }

    private static String extractValue(String supervisorDecision) {
        String text = FeedbackVerdict.withoutThinking(supervisorDecision);
        if (text == null || text.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$[0-9,]+").matcher(text);
        return matcher.find() ? matcher.group() : "";
    }

    private static String compact(Object output) {
        if (output == null) {
            return "";
        }
        String text = FeedbackVerdict.withoutThinking(String.valueOf(output)).replaceAll("\\s+", " ").trim();
        if (text.length() > 400) {
            return text.substring(0, 400) + "…";
        }
        return text;
    }
}
