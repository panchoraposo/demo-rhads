package com.carmanagement.service;

import com.carmanagement.model.ApprovalProposal;
import com.carmanagement.model.ApprovalProposal.ApprovalStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jakarta.transaction.Transactional.TxType;

/**
 * Stores HITL proposals and completes the future the workflow is blocked on.
 */
@ApplicationScoped
public class ApprovalService {

    @Inject
    EntityManager entityManager;

    private final Map<Integer, CompletableFuture<ApprovalProposal>> pendingApprovals = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public CompletableFuture<ApprovalProposal> createProposalAndWaitForDecision(
            Integer carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback) {

        ApprovalProposal existing = findPending(carNumber);
        if (existing != null) {
            Log.warnf("Proposal already exists for car %d, returning existing future", carNumber);
            return pendingApprovals.computeIfAbsent(carNumber, k -> new CompletableFuture<>());
        }

        CompletableFuture<ApprovalProposal> future = new CompletableFuture<>();
        pendingApprovals.put(carNumber, future);

        executor.submit(() -> {
            try {
                createProposalInNewTransaction(carNumber, carMake, carModel, carYear, carValue,
                        proposedDisposition, dispositionReason, carCondition, rentalFeedback);
                Log.info("Proposal creation transaction committed — visible to the UI");
            } catch (Exception e) {
                Log.errorf(e, "Failed to create proposal for car %d", carNumber);
                future.completeExceptionally(e);
                pendingApprovals.remove(carNumber);
            }
        });

        return future;
    }

    @Transactional(TxType.REQUIRES_NEW)
    ApprovalProposal findPending(Integer carNumber) {
        return ApprovalProposal.findPendingByCarNumber(carNumber);
    }

    @Transactional(TxType.REQUIRES_NEW)
    void createProposalInNewTransaction(
            Integer carNumber,
            String carMake,
            String carModel,
            Integer carYear,
            String carValue,
            String proposedDisposition,
            String dispositionReason,
            String carCondition,
            String rentalFeedback) {

        ApprovalProposal proposal = new ApprovalProposal();
        proposal.carNumber = carNumber;
        proposal.carMake = clip(carMake, 255);
        proposal.carModel = clip(carModel, 255);
        proposal.carYear = carYear;
        String displayValue = moneyOrClip(carValue, 255);
        proposal.carValue = displayValue;
        proposal.proposedDisposition = clip(
                proposedDisposition == null || proposedDisposition.isBlank() ? "SCRAP" : proposedDisposition, 32);
        String reason = dispositionReason;
        if (carValue != null && !carValue.isBlank() && !carValue.equals(displayValue)) {
            reason = clip(carValue + (reason == null || reason.isBlank() ? "" : "\n\n" + reason), 2000);
        }
        proposal.dispositionReason = clip(reason, 2000);
        proposal.carCondition = clip(carCondition, 1000);
        proposal.rentalFeedback = clip(rentalFeedback, 2000);
        proposal.status = ApprovalStatus.PENDING;
        proposal.createdAt = LocalDateTime.now();
        proposal.persist();
        entityManager.flush();

        Log.infof("Created approval proposal ID=%d for car %d — %s %s %s (value: %s, proposed: %s)",
                proposal.id, carNumber, carYear, carMake, carModel, carValue, proposedDisposition);
    }

    @Transactional(TxType.REQUIRES_NEW)
    public ApprovalProposal processDecision(Integer proposalId, boolean approved, String reason, String approvedBy) {
        ApprovalProposal proposal = ApprovalProposal.findById(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("Proposal not found: " + proposalId);
        }
        if (proposal.status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Proposal is not pending: " + proposalId);
        }

        proposal.status = approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        proposal.decision = approved ? "APPROVED" : "REJECTED";
        proposal.approvalReason = clip(reason, 1000);
        proposal.approvedBy = clip(approvedBy, 255);
        proposal.decidedAt = LocalDateTime.now();
        proposal.persist();

        Log.infof("Human decision for car %d: %s — %s", proposal.carNumber, proposal.decision, reason);

        CompletableFuture<ApprovalProposal> future = pendingApprovals.remove(proposal.carNumber);
        if (future != null) {
            future.complete(proposal);
        }
        return proposal;
    }

    public List<ApprovalProposal> getPendingProposals() {
        return ApprovalProposal.findAllPending();
    }

    public ApprovalProposal getProposal(Integer proposalId) {
        return ApprovalProposal.findById(proposalId);
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String moneyOrClip(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$[0-9,]+").matcher(value);
        if (matcher.find()) {
            return matcher.group();
        }
        return clip(value, max);
    }
}
