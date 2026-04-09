package com.eventbudget.dto;

import com.eventbudget.model.domain.Budget;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BudgetResponse(
        Long budgetId,
        Long eventId,
        String eventName,
        BigDecimal totalAmount,
        BigDecimal totalAllocated,
        String status,
        List<BudgetCategoryResponse> categories,
        UserSummaryResponse approvedBy,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {

    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getBudgetId(),
                budget.getEvent().getEventId(),
                budget.getEvent().getName(),
                budget.getTotalAmount(),
                budget.getTotalAllocated(),
                budget.getStatus().name(),
                budget.getCategories().stream().map(BudgetCategoryResponse::from).toList(),
                budget.getApprovedBy() != null ? UserSummaryResponse.from(budget.getApprovedBy()) : null,
                budget.getCreatedAt(),
                budget.getClosedAt());
    }
}
