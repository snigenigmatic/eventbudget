package com.eventbudget.dto;

import com.eventbudget.model.domain.ExpenseClaim;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseClaimResponse(
        Long claimId,
        Long budgetId,
        Long budgetCategoryId,
        String expenseCategoryName,
        String vendor,
        BigDecimal amount,
        String description,
        LocalDate expenseDate,
        String status,
        String approvalLevel,
        UserSummaryResponse submittedBy,
        List<ApprovalStepResponse> steps,
        LocalDateTime createdAt
) {

    public static ExpenseClaimResponse from(ExpenseClaim claim) {
        return new ExpenseClaimResponse(
                claim.getClaimId(),
                claim.getBudget().getBudgetId(),
                claim.getBudgetCategory().getCategoryId(),
                claim.getBudgetCategory().getExpenseCategory().getName(),
                claim.getVendor(),
                claim.getAmount(),
                claim.getDescription(),
                claim.getExpenseDate(),
                claim.getStatus().name(),
                claim.getApprovalLevel() != null ? claim.getApprovalLevel().name() : "AUTO_APPROVED",
                UserSummaryResponse.from(claim.getSubmittedBy()),
                claim.getWorkflow() != null
                        ? claim.getWorkflow().getSteps().stream().map(ApprovalStepResponse::from).toList()
                        : List.of(),
                claim.getCreatedAt());
    }
}
