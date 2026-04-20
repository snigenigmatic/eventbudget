package com.eventbudget.service.factory;

import com.eventbudget.exception.BusinessException;
import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import com.eventbudget.service.strategy.ApprovalRoutingStrategy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * FACTORY PATTERN (Creational).
 *
 * <p>Encapsulates the decision of <em>which</em>
 * {@link ApprovalRoutingStrategy} to use for a given claim and hides the
 * construction of the resulting {@link ApprovalWorkflow} from the caller.
 *
 * <p>Spring injects every {@link ApprovalRoutingStrategy} bean at startup;
 * the factory indexes them by {@link ApprovalLevel} in an {@link EnumMap}
 * and looks up the correct strategy at request time based on
 * {@link WorkflowConfig#resolveLevel(java.math.BigDecimal)}.
 *
 * <p>The factory collaborates with:
 * <ul>
 *   <li>STRATEGY PATTERN — it owns and dispatches to the strategies.</li>
 *   <li>OPEN/CLOSED PRINCIPLE — a new strategy bean is automatically
 *       discovered; no change to this class or to callers is required.</li>
 * </ul>
 */
@Component
public class ApprovalWorkflowFactory {

    private final Map<ApprovalLevel, ApprovalRoutingStrategy> strategies;

    public ApprovalWorkflowFactory(List<ApprovalRoutingStrategy> strategies) {
        this.strategies = new EnumMap<>(ApprovalLevel.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.supports(), strategy));
    }

    /**
     * Factory method — chooses the strategy from the claim amount and
     * delegates to it.
     */
    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers) {
        return createWorkflow(claim, config, approvers, config.resolveLevel(claim.getAmount()));
    }

    /**
     * Factory method — overload that lets the caller force a specific
     * {@link ApprovalLevel} (used when a category rule promotes an
     * otherwise-auto-approved claim to single-level).
     */
    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers,
            ApprovalLevel level) {
        ApprovalRoutingStrategy strategy = strategies.get(level);
        if (strategy == null) {
            throw new BusinessException("No approval strategy registered for level: " + level);
        }
        return strategy.createWorkflow(claim, config, approvers);
    }
}
