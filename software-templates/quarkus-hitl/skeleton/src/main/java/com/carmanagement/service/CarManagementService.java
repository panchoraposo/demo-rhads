package com.carmanagement.service;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.carmanagement.agentic.trace.WorkflowResult;
import com.carmanagement.agentic.trace.WorkflowTrace;
import com.carmanagement.agentic.workflow.CarProcessingWorkflow;
import com.carmanagement.model.CarConditions;
import com.carmanagement.model.CarInfo;
import com.carmanagement.model.CarStatus;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class CarManagementService {

    private static final String TYPE = "supervisor";

    @Inject
    CarProcessingWorkflow carProcessingWorkflow;

    public Uni<WorkflowResult> processCarReturn(Integer carNumber, String feedback) {
        return Uni.createFrom().item(() -> run(carNumber, feedback))
                .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
    }

    private WorkflowResult run(Integer carNumber, String feedback) {
        CarInfo carInfo = findCarInfo(carNumber);
        if (carInfo == null) {
            throw new IllegalArgumentException("Car not found with number: " + carNumber);
        }

        WorkflowTrace.start(TYPE, carNumber, feedback);
        CarConditions carConditions = carProcessingWorkflow.processCarReturn(carInfo, carNumber, feedback);
        Map<String, Object> intake = carConditions.intake() != null ? carConditions.intake() : Map.of();
        WorkflowTrace.assignment(TYPE, intake);

        String action = WorkflowTrace.statusFor(intake);
        persistOutcome(carNumber, action, carConditions.generalCondition());
        carInfo = findCarInfo(carNumber);

        WorkflowTrace.applyTools(carNumber, WorkflowTrace.snapshot(carInfo), intake);
        WorkflowTrace.done(TYPE, carNumber, action);
        return WorkflowTrace.response(carInfo, TYPE, intake);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    CarInfo findCarInfo(Integer carNumber) {
        return CarInfo.findById(carNumber);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void persistOutcome(Integer carNumber, String action, String condition) {
        CarInfo carInfo = CarInfo.findById(carNumber);
        carInfo.condition = condition;
        carInfo.status = CarStatus.valueOf(action);
        CarInfo.getEntityManager().merge(carInfo);
    }
}
