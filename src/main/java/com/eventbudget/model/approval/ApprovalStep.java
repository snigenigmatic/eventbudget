package com.eventbudget.model.approval;

import java.time.LocalDateTime;

import com.eventbudget.model.user.ApprovingAuthority;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * CHAIN OF RESPONSIBILITY PATTERN (Behavioral) — Concrete handler.
 *
 * <p>Each {@code ApprovalStep} is one link in an approval chain held by
 * {@link ApprovalWorkflow}. The currently-active step handles an approval
 * request for an {@link com.eventbudget.model.domain.ExpenseClaim}:
 * <ul>
 *   <li>If it decides {@code APPROVED} the workflow forwards the request
 *       to the next handler via {@link ApprovalWorkflow#advance()}.</li>
 *   <li>If it decides {@code REJECTED} the chain terminates.</li>
 * </ul>
 *
 * <p>Using the chain, a multi-level claim (e.g. Coordinator → Dean) is
 * processed without the caller knowing how many approvers exist or who the
 * next approver is.
 */
@Entity
@Table(name = "approval_steps")
@Getter
@Setter
public class ApprovalStep {

    public static final String DECISION_PENDING = "PENDING";
    public static final String DECISION_APPROVED = "APPROVED";
    public static final String DECISION_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = false)
    private ApprovingAuthority assignedTo;

    private int stepOrder;
    private String decision = DECISION_PENDING;
    private String comment;
    private LocalDateTime decidedAt;

    public boolean isDecided() {
        return decision != null && !decision.equals(DECISION_PENDING);
    }

    public boolean isApproved() {
        return DECISION_APPROVED.equals(decision);
    }

    public boolean isRejected() {
        return DECISION_REJECTED.equals(decision);
    }

    /**
     * CHAIN OF RESPONSIBILITY — handle the request at this link in the chain.
     * Records the decision and timestamp; the enclosing {@link ApprovalWorkflow}
     * decides whether to forward (advance to next handler) or terminate.
     *
     * @param decision {@code APPROVED} or {@code REJECTED}
     * @param comment  optional reviewer comment
     */
    public void resolve(String decision, String comment) {
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = LocalDateTime.now();
    }
}
