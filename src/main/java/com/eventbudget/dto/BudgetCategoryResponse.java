package com.eventbudget.dto;

import com.eventbudget.model.domain.BudgetCategory;
import java.math.BigDecimal;

public record BudgetCategoryResponse(
        Long categoryId,
        Long expenseCategoryId,
        String expenseCategoryName,
        BigDecimal allocatedAmount,
        BigDecimal committedAmount,
        BigDecimal approvedExpenditure,
        BigDecimal availableBalance,
        double utilizationPercent
) {

    public static BudgetCategoryResponse from(BudgetCategory category) {
        return new BudgetCategoryResponse(
                category.getCategoryId(),
                category.getExpenseCategory().getCategoryId(),
                category.getExpenseCategory().getName(),
                category.getAllocatedAmount(),
                category.getCommittedAmount(),
                category.getApprovedExpenditure(),
                category.getAvailableBalance(),
                category.getUtilizationPercent());
    }
}
