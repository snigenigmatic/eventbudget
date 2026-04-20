package com.eventbudget.service.strategy;

import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import java.util.List;

/**
 * STRATEGY PATTERN (Behavioral) — Strategy interface.
 *
 * <p>Defines the family of interchangeable algorithms used to build an
 * {@link ApprovalWorkflow} for an {@link ExpenseClaim}. The concrete
 * strategy to apply is selected at runtime based on the claim amount and
 * the active {@link WorkflowConfig}.
 *
 * <p>Concrete strategies:
 * <ul>
 *   <li>{@link AutoApprovalStrategy} — no steps, instant approval.</li>
 *   <li>{@link SingleLevelApprovalStrategy} — one approver step.</li>
 *   <li>{@link MultiLevelApprovalStrategy} — two ordered approver steps
 *       (Coordinator → Dean) which together form the
 *       Chain of Responsibility in {@link ApprovalWorkflow}.</li>
 * </ul>
 *
 * <p>The strategy is chosen by {@link com.eventbudget.service.factory.ApprovalWorkflowFactory},
 * keeping the caller ({@code OrganizerService}) ignorant of which algorithm runs.
 */
public interface ApprovalRoutingStrategy {

    /**
     * @return the {@link ApprovalLevel} this strategy handles.
     */
    ApprovalLevel supports();

    /**
     * Build an {@link ApprovalWorkflow} (with zero or more ordered
     * {@link com.eventbudget.model.approval.ApprovalStep}s) for the given claim.
     */
    ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers);
}
