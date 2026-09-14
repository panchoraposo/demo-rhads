package com.carmanagement.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Disposition proposal waiting for a human decision. The workflow pauses until
 * {@link #status} leaves {@link ApprovalStatus#PENDING}.
 */
@Entity
public class ApprovalProposal extends PanacheEntity {

    @Column(nullable = false)
    public Integer carNumber;

    public String carMake;
    public String carModel;
    public Integer carYear;

    @Column(length = 8000)
    public String carValue;

    @Column(nullable = false, length = 32)
    public String proposedDisposition;

    @Column(length = 8000)
    public String dispositionReason;

    @Column(length = 2000)
    public String carCondition;

    @Column(length = 8000)
    public String rentalFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ApprovalStatus status = ApprovalStatus.PENDING;

    public String decision;

    @Column(length = 2000)
    public String approvalReason;

    public String approvedBy;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime decidedAt;

    public static ApprovalProposal findPendingByCarNumber(Integer carNumber) {
        return find("carNumber = ?1 and status = ?2", carNumber, ApprovalStatus.PENDING).firstResult();
    }

    public static List<ApprovalProposal> findAllPending() {
        return find("status", ApprovalStatus.PENDING).list();
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
