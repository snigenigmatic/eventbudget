package com.eventbudget.service.strategy;

import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import java.util.List;

public interface ApprovalRoutingStrategy {

    ApprovalLevel supports();

    ApprovalWorkflow createWorkflow(
            ExpenseClaim claim,
            WorkflowConfig config,
            List<ApprovingAuthority> approvers);
}
