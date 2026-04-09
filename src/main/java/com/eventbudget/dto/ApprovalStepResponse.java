package com.eventbudget.dto;

import com.eventbudget.model.approval.ApprovalStep;
import java.time.LocalDateTime;

public record ApprovalStepResponse(
        Long stepId,
        int stepOrder,
        String decision,
        String comment,
        LocalDateTime decidedAt,
        UserSummaryResponse assignedTo
) {

    public static ApprovalStepResponse from(ApprovalStep step) {
        return new ApprovalStepResponse(
                step.getStepId(),
                step.getStepOrder(),
                step.getDecision(),
                step.getComment(),
                step.getDecidedAt(),
                UserSummaryResponse.from(step.getAssignedTo()));
    }
}
