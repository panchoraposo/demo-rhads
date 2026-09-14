package com.carmanagement.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;

import com.carmanagement.agentic.trace.WorkflowResult;
import com.carmanagement.service.CarManagementService;

@Path("/car-management")
public class CarManagementResource {

    @Inject
    CarManagementService carManagementService;

    @POST
    @Path("/return/{carNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Response processReturn(Integer carNumber, @RestQuery String feedback) {
        try {
            WorkflowResult result = carManagementService.startCarReturn(
                    carNumber, feedback != null ? feedback : "");
            return Response.accepted(result).build();
        } catch (IllegalArgumentException e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error processing car return: " + e.getMessage()))
                    .build();
        }
    }
}
