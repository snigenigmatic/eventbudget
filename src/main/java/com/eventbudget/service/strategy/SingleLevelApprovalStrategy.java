package com.eventbudget.service.strategy;

import com.eventbudget.exception.BusinessException;
import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalStep;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SingleLevelApprovalStrategy implements ApprovalRoutingStrategy {

    @Override
    public ApprovalLevel supports() {
        return ApprovalLevel.SINGLE_LEVEL;
    }

    @Override
    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers) {
        ApprovingAuthority approver = approvers.stream()
                .filter(candidate -> candidate.canApprove(claim.getAmount()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("No approving authority can approve this claim amount"));

        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setClaim(claim);
        workflow.setConfig(config);

        ApprovalStep step = new ApprovalStep();
        step.setWorkflow(workflow);
        step.setAssignedTo(approver);
        step.setStepOrder(0);
        workflow.getSteps().add(step);
        return workflow;
    }
}
