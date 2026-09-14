package com.carmanagement.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

import com.carmanagement.service.CarManagementService;

@Path("/car-management")
public class CarManagementResource {

    @Inject
    CarManagementService carManagementService;

    @POST
    @Path("/return/{carNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public Uni<Response> processReturn(Integer carNumber, @RestQuery String feedback) {
        return carManagementService.processCarReturn(carNumber, feedback != null ? feedback : "")
                .onItem().transform(result -> Response.ok(result).build())
                .onFailure().recoverWithItem(e -> {
                    Log.error(e.getMessage(), e);
                    Response.Status status = e instanceof IllegalArgumentException
                            ? Response.Status.NOT_FOUND
                            : Response.Status.INTERNAL_SERVER_ERROR;
                    return Response.status(status)
                            .entity("Error processing car return: " + e.getMessage())
                            .build();
                });
    }
}
