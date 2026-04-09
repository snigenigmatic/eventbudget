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

@Component
public class ApprovalWorkflowFactory {

    private final Map<ApprovalLevel, ApprovalRoutingStrategy> strategies;

    public ApprovalWorkflowFactory(List<ApprovalRoutingStrategy> strategies) {
        this.strategies = new EnumMap<>(ApprovalLevel.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.supports(), strategy));
    }

    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers) {
        return createWorkflow(claim, config, approvers, config.resolveLevel(claim.getAmount()));
    }

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
