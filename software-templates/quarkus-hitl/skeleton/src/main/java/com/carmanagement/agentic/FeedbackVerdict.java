package com.carmanagement.agentic;

import com.carmanagement.model.CarAssignment;
import com.carmanagement.model.FeedbackAnalysisResults;

/**
 * Maps agent text to a fleet assignment without trusting chain-of-thought.
 * <p>
 * Routing follows the same priority as Camel EIP:
 * disposition &gt; maintenance &gt; cleaning &gt; available.
 * Human HITL tokens {@code KEEP_CAR}/{@code DISPOSE_CAR} still win in steps 05–07.
 */
public final class FeedbackVerdict {

    private FeedbackVerdict() {
    }

    public static String withoutThinking(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = text.replaceAll("(?is)<think>.*?</think>", "").trim();
        int close = stripped.toLowerCase().lastIndexOf("</think>");
        if (close >= 0) {
            stripped = stripped.substring(close + "</think>".length()).trim();
        }
        return stripped.replaceAll("(?is)</?think>", "").trim();
    }

    public static boolean required(String analysis, String notRequiredToken) {
        String text = withoutThinking(analysis);
        if (text.isEmpty()) {
            return false;
        }
        return !text.toUpperCase().contains(notRequiredToken.toUpperCase());
    }

    public static CarAssignment assignment(FeedbackAnalysisResults analysis, String supervisorDecision) {
        String decision = withoutThinking(supervisorDecision).toUpperCase();
        if (decision.contains("KEEP_CAR") && !decision.contains("DISPOSE_CAR")) {
            if (required(analysis.maintenanceAnalysis(), "MAINTENANCE_NOT_REQUIRED")) {
                return CarAssignment.MAINTENANCE;
            }
            if (required(analysis.cleaningAnalysis(), "CLEANING_NOT_REQUIRED")) {
                return CarAssignment.CLEANING;
            }
            return CarAssignment.NONE;
        }
        if (decision.contains("DISPOSE_CAR") || required(analysis.dispositionAnalysis(), "DISPOSITION_NOT_REQUIRED")) {
            return CarAssignment.DISPOSITION;
        }
        if (required(analysis.maintenanceAnalysis(), "MAINTENANCE_NOT_REQUIRED")) {
            return CarAssignment.MAINTENANCE;
        }
        if (required(analysis.cleaningAnalysis(), "CLEANING_NOT_REQUIRED")) {
            return CarAssignment.CLEANING;
        }
        return CarAssignment.NONE;
    }

    public static String extractDispositionAction(String... texts) {
        StringBuilder combined = new StringBuilder();
        for (String text : texts) {
            combined.append(' ').append(withoutThinking(text));
        }
        String upper = combined.toString().toUpperCase();
        java.util.regex.Matcher marker = java.util.regex.Pattern.compile("__(SCRAP|SELL|DONATE|KEEP)__").matcher(upper);
        if (marker.find()) {
            return marker.group(1);
        }
        String last = "";
        java.util.regex.Matcher word = java.util.regex.Pattern.compile("\\b(SCRAP|SELL|DONATE|KEEP)\\b").matcher(upper);
        while (word.find()) {
            last = word.group(1);
        }
        return last;
    }

    public static String dispositionActionOrScrap(String supervisorDecision, String dispositionAnalysis) {
        String extracted = extractDispositionAction(supervisorDecision, dispositionAnalysis);
        String analysis = withoutThinking(dispositionAnalysis).toUpperCase();
        if ((analysis.contains("WRECK") || analysis.contains("TOTAL") || analysis.contains("DESTROY")
                || analysis.contains("FRAME") || analysis.contains("AIRBAG")
                || analysis.contains("COMPROMISED") || analysis.contains("IN PIECES"))
                && !"KEEP".equals(extracted)) {
            return "SCRAP";
        }
        return extracted.isBlank() ? "SCRAP" : extracted;
    }

    public static String liveCondition(com.carmanagement.model.CarInfo carInfo, String feedback,
            FeedbackAnalysisResults results) {
        String previous = carInfo != null && carInfo.condition != null ? carInfo.condition : "";
        if (results == null) {
            return previous;
        }
        if (required(results.dispositionAnalysis(), "DISPOSITION_NOT_REQUIRED")) {
            String analysis = withoutThinking(results.dispositionAnalysis())
                    .replaceFirst("(?i)^DISPOSITION_REQUIRED:?\\s*", "")
                    .trim();
            if (analysis.isBlank() && feedback != null && !feedback.isBlank()) {
                analysis = feedback.trim();
            }
            return analysis.isBlank() ? "severe damage, uneconomical to repair" : analysis;
        }
        return previous;
    }

    public static String applyLiveCondition(com.carmanagement.model.CarInfo carInfo, String feedback,
            FeedbackAnalysisResults results) {
        String live = liveCondition(carInfo, feedback, results);
        if (carInfo != null && live != null && !live.isBlank()) {
            carInfo.condition = live;
        }
        return live;
    }

    public static String formatDispositionCondition(String action, String condition) {
        String act = action == null ? "" : action.trim().toUpperCase();
        if (!act.equals("SCRAP") && !act.equals("SELL") && !act.equals("DONATE") && !act.equals("KEEP")) {
            act = extractDispositionAction(action, condition);
        }
        if (act.isBlank()) {
            act = "SCRAP";
        }
        String rest = withoutThinking(condition);
        if (!rest.isBlank() && rest.toUpperCase().startsWith(act)) {
            return rest;
        }
        if (rest.isBlank() || rest.toUpperCase().contains("GOOD CONDITION")) {
            rest = "severe damage, repair cost exceeds value";
        }
        return act + " - " + rest;
    }

}
