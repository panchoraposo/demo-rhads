package com.carmanagement.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Record representing the conditions of a car.
 *
 * @param generalCondition  A description of the car's general condition
 * @param carAssignment     Indicates the action required
 * @param dispositionStatus DISPOSITION_APPROVED, DISPOSITION_REJECTED, or DISPOSITION_NOT_REQUIRED
 * @param dispositionReason Reason for the disposition decision
 * @param intake            Camel-compatible intake flags for the JSON envelope
 */
public record CarConditions(
        String generalCondition,
        CarAssignment carAssignment,
        String dispositionStatus,
        String dispositionReason,
        @JsonIgnore Map<String, Object> intake
) {
    public CarConditions(String generalCondition, CarAssignment carAssignment) {
        this(generalCondition, carAssignment, "DISPOSITION_NOT_REQUIRED", null, Map.of());
    }

    public CarConditions(
            String generalCondition,
            CarAssignment carAssignment,
            String dispositionStatus,
            String dispositionReason) {
        this(generalCondition, carAssignment, dispositionStatus, dispositionReason, Map.of());
    }
}
