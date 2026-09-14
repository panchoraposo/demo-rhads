package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.declarative.SupervisorAgent;
import dev.langchain4j.agentic.declarative.SupervisorRequest;

/**
 * Supervisor that routes high-value dispositions through a human reviewer.
 */
public interface FleetSupervisorAgent {

    @SupervisorAgent(
            outputKey = "supervisorDecision",
            maxAgentsInvocations = 6,
            subAgents = {
                    PricingAgent.class,
                    DispositionProposalAgent.class,
                    HumanApprovalAgent.class,
                    DispositionAgent.class,
                    MaintenanceAgent.class,
                    CleaningAgent.class
            }
    )
    String superviseCarProcessing(
            CarInfo carInfo,
            Integer carNumber,
            String feedback,
            FeedbackAnalysisResults feedbackAnalysisResults
    );

    @SupervisorRequest
    static String request(
            CarInfo carInfo,
            Integer carNumber,
            String feedback,
            FeedbackAnalysisResults feedbackAnalysisResults
    ) {
        boolean dispositionRequired = com.carmanagement.agentic.FeedbackVerdict.required(
                feedbackAnalysisResults.dispositionAnalysis(), "DISPOSITION_NOT_REQUIRED");
        String currentCondition = com.carmanagement.agentic.FeedbackVerdict.applyLiveCondition(
                carInfo, feedback, feedbackAnalysisResults);

        String noDispositionMessage = """
               No disposition has been requested.
               
                INSTRUCTIONS:
                - DO NOT invoke PricingAgent
                - DO NOT invoke DispositionProposalAgent
                - DO NOT invoke HumanApprovalAgent
                - DO NOT invoke DispositionAgent
                - Only invoke MaintenanceAgent if maintenance needed
                - Only invoke CleaningAgent if cleaning needed
               """;

        String dispositionMessage = """
            DISPOSITION_REQUIRED
            
            Follow these steps:
            
            1. Invoke PricingAgent (keep $ format in carValue). Pass carCondition as Current Condition below, not the old fleet record.
            2. IF value > $15,000 (HIGH-VALUE):
               - MUST invoke DispositionProposalAgent then HumanApprovalAgent (do NOT invoke DispositionAgent)
               - If HumanApprovalAgent reason contains KEEP_CAR → end with KEEP_CAR
               - If HumanApprovalAgent reason contains DISPOSE_CAR → end with DISPOSE_CAR
               - If HumanApprovalAgent says REJECTED without KEEP_CAR/DISPOSE_CAR → end with KEEP_CAR
            3. IF value ≤ $15,000 (LOW-VALUE):
               - Invoke DispositionAgent directly (skip HumanApprovalAgent)
               - KEEP → KEEP_CAR; SCRAP/SELL/DONATE → DISPOSE_CAR
            4. IF KEEP_CAR: invoke MaintenanceAgent/CleaningAgent as needed
            
            CRITICAL: End with KEEP_CAR or DISPOSE_CAR
            """;

        return String.format("""
            You are a fleet supervisor for a car rental company. You coordinate action agents based on feedback analysis.
            
            The feedback has already been analyzed and you have these inputs:
            - cleaningAnalysis: What cleaning is needed (or "CLEANING_NOT_REQUIRED")
            - maintenanceAnalysis: What maintenance is needed (or "MAINTENANCE_NOT_REQUIRED")
            - dispositionAnalysis: Whether severe damage requires disposition (or "DISPOSITION_NOT_REQUIRED")
            
            Your job is to invoke the appropriate ACTION agents for this car
            
            Car: %d %s %s (#%d)
            Current Condition: %s
            Feedback: %s
            
            Cleaning Analysis: %s
            Maintenance Analysis: %s
            Disposition Analysis: %s
            
            In particular, you have to follow these steps
            
            %s
            
            /no_think
            """,
                carInfo.year, carInfo.make, carInfo.model, carNumber, currentCondition, feedback,
                feedbackAnalysisResults.cleaningAnalysis(),
                feedbackAnalysisResults.maintenanceAnalysis(),
                feedbackAnalysisResults.dispositionAnalysis(),
                dispositionRequired ? dispositionMessage : noDispositionMessage);
    }
}
