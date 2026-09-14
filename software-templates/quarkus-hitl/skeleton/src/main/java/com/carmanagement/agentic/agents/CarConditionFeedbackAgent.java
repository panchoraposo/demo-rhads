package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine the final car condition and assignment.
 * This is the final decision-maker that interprets all previous agent outputs.
 */
public interface CarConditionFeedbackAgent {

    @SystemMessage("""
        Analyze car processing results and output a JSON summary.
        
        Output format:
        {
          "generalCondition": "concise description (max 200 chars)",
          "carAssignment": "DISPOSITION|MAINTENANCE|CLEANING|NONE"
        }
        
        Rules:
        - carAssignment: DISPOSE_CAR → DISPOSITION
        - KEEP_CAR + maintenance needed → MAINTENANCE
        - KEEP_CAR + cleaning needed → CLEANING
        - KEEP_CAR + none → NONE
        - If supervisorDecision mentions SCRAP/SELL/DONATE (but NOT KEEP / KEEP_CAR) → DISPOSITION
        - Else if maintenanceAnalysis ≠ "MAINTENANCE_NOT_REQUIRED" → MAINTENANCE
        - Else if cleaningAnalysis ≠ "CLEANING_NOT_REQUIRED" → CLEANING
        - Else → NONE
        - generalCondition: Summarize the action and reason
        - If DISPOSITION: generalCondition MUST be "<ACTION> - <reason>", e.g. "SCRAP - severe damage, repair cost exceeds value"
        """)
    @UserMessage("""
            Car: {carInfo.year} {carInfo.make} {carInfo.model} (#{carNumber})
            
            Supervisor Decision: {supervisorDecision}
            
            Feedback Analysis Results:
            - Disposition: {feedbackAnalysisResults.dispositionAnalysis}
            - Maintenance: {feedbackAnalysisResults.maintenanceAnalysis}
            - Cleaning: {feedbackAnalysisResults.cleaningAnalysis}
            """)
    @Agent(description = "Final car condition analyzer. Determines the car's condition and assignment based on all feedback.",
            outputKey = "carConditions")
    CarConditions analyzeForCondition(
            CarInfo carInfo,
            Integer carNumber,
            FeedbackAnalysisResults feedbackAnalysisResults,
            String supervisorDecision);
}
