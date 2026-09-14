package com.carmanagement.agentic.workflow;

import com.carmanagement.agentic.agents.CleaningFeedbackAgent;
import com.carmanagement.agentic.agents.DispositionFeedbackAgent;
import com.carmanagement.agentic.agents.MaintenanceFeedbackAgent;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.FeedbackAnalysisResults;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;

/**
 * Parallel feedback analysis for cleaning, maintenance, and disposition.
 * <p>
 * The upstream workshop uses {@code @ParallelMapperAgent} (LangChain4j 1.12+) to run one
 * parameterized agent three times. Red Hat build of Quarkus 3.33 ships LangChain4j 1.11,
 * so this demo keeps the same idea with three specialist agents under {@code @ParallelAgent}.
 */
public interface FeedbackWorkflow {

    @ParallelAgent(outputKey = "feedbackAnalysisResults",
            subAgents = { CleaningFeedbackAgent.class, MaintenanceFeedbackAgent.class, DispositionFeedbackAgent.class })
    FeedbackAnalysisResults analyzeFeedback(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);

    @Output
    static FeedbackAnalysisResults output(String cleaningAnalysis, String maintenanceAnalysis, String dispositionAnalysis) {
        return new FeedbackAnalysisResults(cleaningAnalysis, maintenanceAnalysis, dispositionAnalysis);
    }
}
