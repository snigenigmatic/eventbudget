package com.eventbudget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseClaimRequest(
        @NotNull Long budgetId,
        @NotNull Long budgetCategoryId,
        @NotBlank String vendor,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String description,
        @NotNull LocalDate expenseDate,
        List<@Valid SupportingDocumentRequest> documents
) {
}
