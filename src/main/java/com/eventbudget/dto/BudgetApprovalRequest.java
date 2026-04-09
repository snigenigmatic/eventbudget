package com.eventbudget.dto;

public record BudgetApprovalRequest(
        boolean approved,
        String comment
) {
}
