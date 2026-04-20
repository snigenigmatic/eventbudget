package com.eventbudget.model.approval;

import com.eventbudget.model.domain.ExpenseClaim;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

/**
 * CHAIN OF RESPONSIBILITY PATTERN (Behavioral) — Chain owner / dispatcher.
 *
 * <p>An {@code ApprovalWorkflow} is the concrete chain of {@link ApprovalStep}
 * handlers that process one {@link ExpenseClaim}. The list of steps is
 * ordered by {@code stepOrder}; {@link #currentStepIndex} points to the
 * handler that currently owns the request.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #getCurrentStep()} — return the active handler.</li>
 *   <li>The handler calls {@link ApprovalStep#resolve(String, String)}.</li>
 *   <li>If approved, {@link #advance()} forwards the request to the next
 *       handler in the chain. If {@link #isComplete()} is then {@code true},
 *       the claim is fully approved.</li>
 *   <li>If rejected, the chain terminates immediately.</li>
 * </ol>
 *
 * <p>Because callers never address a specific handler, new approval tiers
 * (Committee, Registrar, VC) can be inserted by building a longer chain
 * without any change to the calling code.
 */
@Entity
@Table(name = "approval_workflows")
@Getter
@Setter
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workflowId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaim claim;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    private int currentStepIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private WorkflowConfig config;

    /**
     * @return the handler currently responsible for the claim, or {@code null}
     *         if the chain is empty or completed.
     */
    public ApprovalStep getCurrentStep() {
        if (steps.isEmpty() || currentStepIndex >= steps.size()) return null;
        return steps.get(currentStepIndex);
    }

    /**
     * Forward the request to the next handler in the chain.
     * Called after the current handler approves.
     */
    public void advance() {
        this.currentStepIndex++;
    }

    /**
     * @return {@code true} when every handler in the chain has approved.
     */
    public boolean isComplete() {
        return currentStepIndex >= steps.size();
    }

    public boolean isMultiLevel() {
        return steps.size() > 1;
    }
}
