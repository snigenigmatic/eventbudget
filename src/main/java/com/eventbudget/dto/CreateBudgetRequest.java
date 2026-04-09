package com.eventbudget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateBudgetRequest(
        @NotNull Long eventId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @NotEmpty List<@Valid BudgetAllocationRequest> categories
) {
}
