package com.carmanagement.quarkus;

import com.carmanagement.FleetStore;
import com.carmanagement.WorkOrders;
import com.carmanagement.WorkflowSupport;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Camel Quarkus CDI producers. JBang skips this package (see scripts/lib-run.sh).
 */
@ApplicationScoped
@Unremovable
public class QuarkusCamelBeans {

    @Produces
    @Named("fleetStore")
    @ApplicationScoped
    @Unremovable
    FleetStore fleetStore() {
        return new FleetStore();
    }

    @Produces
    @Named("workOrders")
    @ApplicationScoped
    @Unremovable
    WorkOrders workOrders() {
        return new WorkOrders();
    }

    @Produces
    @Named("workflowSupport")
    @ApplicationScoped
    @Unremovable
    WorkflowSupport workflowSupport() {
        return new WorkflowSupport();
    }
}
