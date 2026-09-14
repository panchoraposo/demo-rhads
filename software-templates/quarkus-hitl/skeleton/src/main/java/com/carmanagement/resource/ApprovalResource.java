package com.carmanagement.resource;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.service.ApprovalService;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/approvals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject
    ApprovalService approvalService;

    @GET
    @Path("/pending")
    public List<ApprovalProposal> getPendingProposals() {
        return approvalService.getPendingProposals();
    }

    @GET
    @Path("/{proposalId}")
    public Response getProposal(@PathParam("proposalId") Integer proposalId) {
        ApprovalProposal proposal = approvalService.getProposal(proposalId);
        if (proposal == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Proposal not found"))
                    .build();
        }
        return Response.ok(proposal).build();
    }

    @POST
    @Path("/{proposalId}/approve")
    public Response approveProposal(@PathParam("proposalId") Integer proposalId, Map<String, String> request) {
        return decide(proposalId, true,
                request.getOrDefault("reason", "Approved by human reviewer"),
                request.getOrDefault("approvedBy", "Workshop User"));
    }

    @POST
    @Path("/{proposalId}/reject")
    public Response rejectProposal(@PathParam("proposalId") Integer proposalId, Map<String, String> request) {
        return decide(proposalId, false,
                request.getOrDefault("reason", "Rejected by human reviewer"),
                request.getOrDefault("approvedBy", "Workshop User"));
    }

    /**
     * UI shortcut: the reviewer picks KEEP_CAR or DISPOSE_CAR directly.
     * The workflow reads that token from the approval reason.
     */
    @POST
    @Path("/{proposalId}/decide")
    public Response decideProposal(@PathParam("proposalId") Integer proposalId, Map<String, String> request) {
        String decision = request.get("decision");
        if (decision == null || (!decision.equals("KEEP_CAR") && !decision.equals("DISPOSE_CAR"))) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Decision must be either KEEP_CAR or DISPOSE_CAR"))
                    .build();
        }
        String reason = request.getOrDefault("reason", "Decision by human reviewer");
        String approvedBy = request.getOrDefault("approvedBy", "Workshop User");
        return decide(proposalId, true, decision + ": " + reason, approvedBy);
    }

    private Response decide(Integer proposalId, boolean approved, String reason, String approvedBy) {
        try {
            Log.infof("HITL decision for proposal %d by %s: approved=%s", proposalId, approvedBy, approved);
            return Response.ok(approvalService.processDecision(proposalId, approved, reason, approvedBy)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", e.getMessage())).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            Log.error("Error processing approval", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error processing approval: " + e.getMessage()))
                    .build();
        }
    }
}
