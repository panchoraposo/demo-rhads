package com.carmanagement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;

@BindToRegistry("workflowSupport")
public class WorkflowSupport {

    private static final Set<String> WORKFLOWS = Set.of("sequential", "parallel", "conditional", "loop", "supervisor");

    public String extractJson(String body) {
        if (body == null || body.isBlank()) {
            return "{}";
        }
        String text = body.trim();
        text = text.replaceAll("(?is)<think>.*?</think>", "").trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int startFence = text.indexOf('\n', fence);
            int endFence = text.indexOf("```", fence + 3);
            if (startFence > 0 && endFence > startFence) {
                text = text.substring(startFence + 1, endFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    public String normalizeWorkflow(Object type) {
        if (type == null) {
            return "sequential";
        }
        String value = String.valueOf(type).trim().toLowerCase();
        return WORKFLOWS.contains(value) ? value : "sequential";
    }

    public static boolean flag(Map<String, Object> map, String key) {
        if (map == null) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
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

    public void initIntake(Exchange exchange) {
        Map<String, Object> intake = new ConcurrentHashMap<>();
        intake.put("cleaningRequired", false);
        intake.put("maintenanceRequired", false);
        intake.put("dispositionRequired", false);
        intake.put("cleaningServices", List.of());
        intake.put("maintenanceServices", List.of());
        intake.put("cleaningNotes", "");
        intake.put("maintenanceNotes", "");
        intake.put("dispositionNotes", "");
        intake.put("dispositionAction", "");
        intake.put("carValue", "");
        intake.put("condition", "");
        exchange.setProperty("intake", intake);
        exchange.setProperty("cleaningRequired", false);
        exchange.setProperty("maintenanceRequired", false);
        exchange.setProperty("dispositionRequired", false);
    }

    public void captureCleaning(Exchange exchange) {
        mergeDepartment(exchange, "cleaning");
    }

    public void captureMaintenance(Exchange exchange) {
        mergeDepartment(exchange, "maintenance");
    }

    public void captureDisposition(Exchange exchange) {
        mergeDepartment(exchange, "disposition");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = exchange.getMessage().getBody(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        if (body != null && body.get("dispositionAction") != null) {
            intake.put("dispositionAction", String.valueOf(body.get("dispositionAction")));
        }
        rejectRepairableDisposition(exchange, intake);
    }

    static boolean catastrophicFeedback(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return false;
        }
        String text = feedback.toLowerCase();
        return text.contains("wreck")
                || text.contains("totaled")
                || text.contains("totalled")
                || text.contains("total loss")
                || text.contains("destroy")
                || text.contains("crash")
                || text.contains("collision")
                || text.contains("airbag")
                || text.contains("frame")
                || text.contains("structural")
                || text.contains("in pieces")
                || text.contains("write-off")
                || text.contains("write off");
    }

    private void rejectRepairableDisposition(Exchange exchange, Map<String, Object> intake) {
        if (intake == null || !flag(intake, "dispositionRequired")) {
            return;
        }
        Object rawFeedback = exchange.getProperty("feedback");
        String feedback = rawFeedback == null ? "" : String.valueOf(rawFeedback);
        if (catastrophicFeedback(feedback)) {
            return;
        }
        intake.put("dispositionRequired", false);
        intake.put("dispositionAction", "KEEP");
        intake.put("dispositionNotes", "DISPOSITION_NOT_REQUIRED");
        exchange.setProperty("dispositionRequired", false);
        org.slf4j.LoggerFactory.getLogger("com.carmanagement.agentic.trace.WorkflowTrace")
                .info("[ai] DispositionFeedback overridden: repairable, not catastrophic");
    }

    @SuppressWarnings("unchecked")
    private void mergeDepartment(Exchange exchange, String department) {
        Map<String, Object> body = exchange.getMessage().getBody(Map.class);
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        if (intake == null) {
            initIntake(exchange);
            intake = exchange.getProperty("intake", Map.class);
        }
        boolean required = flag(body, department + "Required");
        intake.put(department + "Required", required);
        if (body != null && body.get(department + "Services") != null) {
            intake.put(department + "Services", body.get(department + "Services"));
        }
        if (body != null && body.get(department + "Notes") != null) {
            intake.put(department + "Notes", String.valueOf(body.get(department + "Notes")));
        }
        if (body != null && body.get("condition") != null && !String.valueOf(body.get("condition")).isBlank()) {
            intake.put("condition", String.valueOf(body.get("condition")));
        }
        mark(exchange, department + "-feedback");
    }

    public void applyIntakeFlags(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        boolean cleaning = flag(intake, "cleaningRequired");
        boolean maintenance = flag(intake, "maintenanceRequired");
        boolean disposition = flag(intake, "dispositionRequired");
        exchange.setProperty("cleaningRequired", cleaning);
        exchange.setProperty("maintenanceRequired", maintenance);
        exchange.setProperty("dispositionRequired", disposition);
        String assignment;
        if (disposition) {
            assignment = "DISPOSITION";
        } else if (maintenance) {
            assignment = "MAINTENANCE";
        } else if (cleaning) {
            assignment = "CLEANING";
        } else {
            assignment = "NONE";
        }
        exchange.setProperty("assignment", assignment);
        String agent = switch (assignment) {
        case "DISPOSITION" -> "PricingAgent+DispositionAgent";
        case "MAINTENANCE" -> "MaintenanceAgent";
        case "CLEANING" -> "CleaningAgent";
        default -> "(none)";
        };
        exchange.setProperty("assignmentAgent", agent);
        exchange.getMessage().setHeader("assignment", assignment);
        String route = switch (assignment) {
        case "DISPOSITION" -> "direct:supervisor-disposition";
        case "MAINTENANCE" -> "direct:tool-request-maintenance";
        case "CLEANING" -> "direct:tool-request-cleaning";
        default -> "direct:supervisor-available";
        };
        exchange.setProperty("assignmentRoute", route);
        resolveCondition(intake);
        mark(exchange, "intake");
    }

    @SuppressWarnings("unchecked")
    public void captureIntake(Exchange exchange) {
        Map<String, Object> intake = exchange.getMessage().getBody(Map.class);
        if (intake == null) {
            intake = Map.of();
        }
        exchange.setProperty("intake", intake);
        exchange.setProperty("cleaningRequired", flag(intake, "cleaningRequired"));
        exchange.setProperty("maintenanceRequired", flag(intake, "maintenanceRequired"));
        mark(exchange, "intake");
    }

    public void mark(Exchange exchange, String step) {
        @SuppressWarnings("unchecked")
        List<String> steps = exchange.getProperty("executedSteps", List.class);
        if (steps == null) {
            steps = new ArrayList<>();
            exchange.setProperty("executedSteps", steps);
        }
        steps.add(step);
    }

    public String joinCleaningServices(Exchange exchange) {
        return joinServices(exchange.getProperty("intake", Map.class), "cleaningServices");
    }

    @SuppressWarnings("unchecked")
    public static String joinServices(Map<String, Object> intake, String key) {
        if (intake == null || !(intake.get(key) instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !String.valueOf(item).isBlank()) {
                parts.add(String.valueOf(item).trim());
            }
        }
        return String.join(",", parts);
    }

    static void resolveCondition(Map<String, Object> intake) {
        if (intake == null) {
            return;
        }
        if (flag(intake, "dispositionRequired")) {
            String disposition = departmentCondition(intake, null, "dispositionNotes", "Severe damage");
            if (!disposition.isBlank()) {
                intake.put("condition", disposition);
            }
            return;
        }
        if (flag(intake, "maintenanceRequired")) {
            intake.put("condition", departmentCondition(intake, "maintenanceServices", "maintenanceNotes",
                    "Needs maintenance"));
            return;
        }
        if (flag(intake, "cleaningRequired")) {
            String cleaning = firstMeaningful(intake.get("condition"),
                    departmentCondition(intake, "cleaningServices", "cleaningNotes", "Needs cleaning"));
            intake.put("condition", cleaning.isBlank() ? "Needs cleaning" : cleaning);
        }
    }

    static String departmentCondition(Map<String, Object> intake, String servicesKey, String notesKey,
            String fallback) {
        if (servicesKey != null) {
            String services = joinServices(intake, servicesKey);
            if (!services.isBlank()) {
                return services.replace(",", ", ");
            }
        }
        String notes = firstMeaningful(intake.get(notesKey));
        if (!notes.isBlank()) {
            int period = notes.indexOf('.');
            if (period > 8 && period < 160) {
                return notes.substring(0, period).trim();
            }
            return notes.length() > 160 ? notes.substring(0, 160).trim() : notes;
        }
        return fallback;
    }

    static String firstMeaningful(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isBlank() || text.toUpperCase().contains("NOT_REQUIRED")) {
                continue;
            }
            return text;
        }
        return "";
    }

    public Map<String, Object> cleaningWorkOrder(Exchange exchange) {
        return departmentOrder(exchange, "cleaning", "cleaningServices", "cleaningNotes", null);
    }


    public void logCleaningTool(org.apache.camel.Exchange exchange) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> car = exchange.getProperty("car", java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> intake = exchange.getProperty("intake", java.util.Map.class);
        org.slf4j.LoggerFactory.getLogger("com.carmanagement.agentic.trace.WorkflowTrace")
                .info("[tool] CleaningTool result: {}", cleaningToolResult(exchange.getProperty("carNumber"), car, intake));
    }

    static String cleaningToolResult(Object carNumber, java.util.Map<String, Object> car,
            java.util.Map<String, Object> intake) {
        if (car == null) {
            car = java.util.Map.of();
        }
        if (intake == null) {
            intake = java.util.Map.of();
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
        if (services instanceof java.util.List<?> list) {
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


    public Map<String, Object> maintenanceWorkOrder(Exchange exchange) {
        return departmentOrder(exchange, "maintenance", "maintenanceServices", "maintenanceNotes", null);
    }

    public Map<String, Object> dispositionWorkOrder(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> car = exchange.getProperty("car", Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("department", "disposition");
        order.put("carNumber", exchange.getProperty("carNumber"));
        order.put("make", car.get("make"));
        order.put("model", car.get("model"));
        order.put("year", car.get("year"));
        Object actionObj = intake.get("dispositionAction");
        String action = actionObj == null ? "" : String.valueOf(actionObj).trim();
        if (action.isBlank()) {
            action = "SCRAP";
            intake.put("dispositionAction", action);
        }
        String notes = intake.get("dispositionNotes") == null ? "" : String.valueOf(intake.get("dispositionNotes"));
        String reason = notes.isBlank() || notes.toUpperCase().contains("DISPOSITION_NOT_REQUIRED")
                ? "severe damage, repair cost exceeds value"
                : notes;
        if (!reason.toUpperCase().startsWith(action.toUpperCase())) {
            intake.put("condition", action + " - " + reason);
        }
        order.put("action", action);
        order.put("estimatedValue", intake.get("carValue"));
        order.put("notes", intake.get("dispositionNotes"));
        order.put("requestedAt", Instant.now().toString());
        mark(exchange, "disposition-tool");
        return order;
    }

    public void pricingEstimate(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> car = exchange.getProperty("car", Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        String make = String.valueOf(car.get("make")).toLowerCase();
        int year = Integer.parseInt(String.valueOf(car.get("year")));
        int age = Math.max(0, java.time.Year.now().getValue() - year);
        int base = 35000;
        if (make.contains("bmw") || make.contains("mercedes") || make.contains("audi") || make.contains("tesla")) {
            base = 60000;
        }
        int value = (int) (base * Math.max(0.3, 1.0 - age * 0.12));
        if (flag(intake, "dispositionRequired")) {
            value = (int) (value * 0.35);
        }
        String formatted = "$" + String.format("%,d", value);
        intake.put("carValue", formatted);
        exchange.setProperty("carValue", formatted);
        mark(exchange, "pricing-agent");
    }

    public void capturePricing(Exchange exchange) {
        String body = exchange.getMessage().getBody(String.class);
        if (body == null) {
            body = "";
        }
        String text = body.replaceAll("(?is)<think>.*?</think>", "").replaceAll("\\s+", " ").trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$[0-9,]+").matcher(text);
        if (matcher.find()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> intake = exchange.getProperty("intake", Map.class);
            String formatted = matcher.group();
            intake.put("carValue", formatted);
            intake.put("pricingJustification", text.length() > 400 ? text.substring(0, 400) + "…" : text);
            exchange.setProperty("carValue", formatted);
            mark(exchange, "pricing-agent");
            return;
        }
        pricingEstimate(exchange);
    }

    public Map<String, Object> cleaningItemWorkOrder(Exchange exchange) {
        String service = String.valueOf(exchange.getMessage().getBody());
        exchange.setProperty("currentService", service.replaceAll("[^a-zA-Z0-9]+", "-"));
        return departmentOrder(exchange, "cleaning", "cleaningServices", "cleaningNotes", service);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> departmentOrder(Exchange exchange, String department, String servicesKey,
            String notesKey, String singleService) {
        Map<String, Object> car = exchange.getProperty("car", Map.class);
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("department", department);
        order.put("carNumber", exchange.getProperty("carNumber"));
        order.put("make", car.get("make"));
        order.put("model", car.get("model"));
        order.put("year", car.get("year"));
        if (singleService != null && !singleService.isBlank()) {
            order.put("services", List.of(singleService));
        } else {
            order.put("services", intake.get(servicesKey));
        }
        order.put("notes", intake.get(notesKey));
        order.put("requestedAt", Instant.now().toString());
        mark(exchange, department + "-tool");
        return order;
    }

    public Map<String, Object> response(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> car = exchange.getMessage().getBody(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> intake = exchange.getProperty("intake", Map.class);
        String type = String.valueOf(exchange.getProperty("workflowType"));
        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("type", type);
        workflow.put("action", car.get("status"));
        workflow.put("intake", intake);
        workflow.put("steps", inferredSteps(type, intake));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("car", car);
        result.put("workflow", workflow);
        return result;
    }

    @SuppressWarnings("unchecked")
    public void bindRequest(Exchange exchange) {
        Map<String, Object> request = bodyMap(exchange.getMessage().getBody());
        Object carNumber = first(request.get("carNumber"), exchange.getMessage().getHeader("carNumber"),
                exchange.getProperty("carNumber"));
        Object feedback = first(request.get("feedback"), exchange.getMessage().getHeader("feedback"),
                exchange.getProperty("feedback"));
        Object workflow = first(request.get("workflow"), exchange.getMessage().getHeader("workflow"),
                exchange.getProperty("workflowType"));
        if (carNumber == null) {
            throw new IllegalArgumentException("carNumber is required");
        }
        exchange.setProperty("carNumber", carNumber);
        exchange.setProperty("feedback", feedback == null ? "" : String.valueOf(feedback));
        exchange.setProperty("workflowType", normalizeWorkflow(workflow));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> bodyMap(Object body) {
        if (body instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Object first(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<String> inferredSteps(String type, Map<String, Object> intake) {
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
        case "loop" -> {
            if (cleaning) {
                steps.add("loop-services");
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
}
