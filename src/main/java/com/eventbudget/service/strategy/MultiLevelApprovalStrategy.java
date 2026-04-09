package com.eventbudget.service.strategy;

import com.eventbudget.exception.BusinessException;
import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalStep;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MultiLevelApprovalStrategy implements ApprovalRoutingStrategy {

    @Override
    public ApprovalLevel supports() {
        return ApprovalLevel.MULTI_LEVEL;
    }

    @Override
    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers) {
        if (approvers.size() < 2) {
            throw new BusinessException("At least two approving authorities are required for multi-level approval");
        }

        ApprovingAuthority finalApprover = approvers.stream()
                .filter(candidate -> candidate.canApprove(claim.getAmount()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No approving authority can approve this claim amount"));

        List<ApprovingAuthority> route = new ArrayList<>();
        route.add(approvers.get(0));
        if (!finalApprover.getUserId().equals(route.get(0).getUserId())) {
            route.add(finalApprover);
        } else if (approvers.size() > 1) {
            route.add(approvers.get(1));
        }

        if (route.size() < 2) {
            throw new BusinessException("Unable to create a multi-level route for this claim");
        }

        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setClaim(claim);
        workflow.setConfig(config);

        for (int index = 0; index < route.size(); index++) {
            ApprovalStep step = new ApprovalStep();
            step.setWorkflow(workflow);
            step.setAssignedTo(route.get(index));
            step.setStepOrder(index);
            workflow.getSteps().add(step);
        }
        return workflow;
    }
}
