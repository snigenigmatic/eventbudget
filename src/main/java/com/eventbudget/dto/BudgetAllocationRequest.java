package com.eventbudget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BudgetAllocationRequest(
        @NotNull Long expenseCategoryId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal allocatedAmount
) {
}
