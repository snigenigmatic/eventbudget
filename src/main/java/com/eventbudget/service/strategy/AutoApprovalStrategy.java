package com.eventbudget.service.strategy;

import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AutoApprovalStrategy implements ApprovalRoutingStrategy {

    @Override
    public ApprovalLevel supports() {
        return ApprovalLevel.AUTO_APPROVED;
    }

    @Override
    public ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers) {
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setClaim(claim);
        workflow.setConfig(config);
        return workflow;
    }
}
