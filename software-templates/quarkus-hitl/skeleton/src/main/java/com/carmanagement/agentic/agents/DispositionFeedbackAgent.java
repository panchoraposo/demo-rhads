package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if a car should leave the fleet.
 */
public interface DispositionFeedbackAgent {

    @SystemMessage("""
        You are a disposition analyzer for a car rental company.
        Respond DISPOSITION_REQUIRED for severe issues that make the car uneconomical to keep:
        wrecked, totaled, destroyed, crashed, collision with major damage, frame/structural damage,
        airbags deployed, unsafe, not drivable. Then the action is SCRAP.
        Minor scratches or dirt is DISPOSITION_NOT_REQUIRED.
        If you detect ANY of these severe issues, respond with:
        "DISPOSITION_REQUIRED: [brief description of the severe issue]"
        If the car has only minor or moderate issues that can be repaired, respond with:
        "DISPOSITION_NOT_REQUIRED"
        Keep your response concise. Do not emit <think> tags.
        """)
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Previous Condition: {carInfo.condition}

        Feedback: {feedback}
        """)
    @Agent(description = "Disposition analyzer. Detects severe damage that may require removing the car from the fleet.",
            outputKey = "dispositionAnalysis")
    String analyzeForDisposition(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);
}
