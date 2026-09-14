package com.carmanagement.agentic.agents;

import com.carmanagement.model.CarInfo;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Agent that analyzes feedback to determine if maintenance is needed.
 */
public interface MaintenanceFeedbackAgent {

    @SystemMessage("""
        You are a car maintenance analyzer for a car rental company.
        Maintenance never includes cleaning or detailing.
        Mechanical issues, strange noises, engine noise, knocking, oil leaks, brakes grinding,
        performance problems, or moderate body damage means maintenance is needed even if the car is also dirty.
        Dirt, mud, stains, hair, or odor alone is NOT maintenance — respond with MAINTENANCE_NOT_REQUIRED.
        If the car is totaled, destroyed, or airbags deployed from a serious collision, respond with
        MAINTENANCE_NOT_REQUIRED — that is disposition, not a workshop repair.
        Be specific about oil change, tires, brakes, engine, transmission, or body work when needed.
        If no service is needed, respond with "MAINTENANCE_NOT_REQUIRED".
        Keep the response short. Do not emit <think> tags.
        """)
    @UserMessage("""
        Car Information:
        Make: {carInfo.make}
        Model: {carInfo.model}
        Year: {carInfo.year}
        Previous Condition: {carInfo.condition}

        Feedback: {feedback}
        """)
    @Agent(description = "Car maintenance analyzer. Using feedback, determines if a car needs maintenance.",
            outputKey = "maintenanceAnalysis")
    String analyzeForMaintenance(
            CarInfo carInfo,
            Integer carNumber,
            String feedback);
}
