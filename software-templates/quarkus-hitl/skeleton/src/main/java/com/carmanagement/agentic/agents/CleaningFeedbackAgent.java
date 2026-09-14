package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if a cleaning is needed.
 */
public interface CleaningFeedbackAgent {

    @SystemMessage("""
        You are a cleaning analyzer for a car rental company.
        Decide if the car needs cleaning based on feedback. Maintenance never cleans cars.
        Collision, totaled, destroyed, airbags, or frame damage is NOT cleaning.
        Dirt, mud, stains, dog hair, pet hair, odor, or "dirty" means cleaning is needed.
        Be specific about exterior, interior, detailing, or waxing when recommending a clean.
        If cleaning is needed, do not write CLEANING_NOT_REQUIRED anywhere in the answer.
        If no interior or exterior cleaning is needed, respond with exactly "CLEANING_NOT_REQUIRED"
        and a one-sentence reason. Do not emit <think> tags.
        """)
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Previous Condition: {carInfo.condition}

        Feedback: {feedback}
        """)
    @Agent(description = "Cleaning analyzer. Using feedback, determines if a cleaning is needed.",
            outputKey = "cleaningAnalysis")
    String analyzeForCleaning(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);
}
