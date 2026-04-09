package com.eventbudget.dto;

import com.eventbudget.model.domain.ExpenseCategory;
import java.math.BigDecimal;

public record ExpenseCategoryResponse(
        Long categoryId,
        String name,
        String description,
        BigDecimal maxAmount,
        boolean mandatoryApproval,
        boolean mandatoryDocumentRequired
) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        BigDecimal maxAmount = category.getRules().stream()
                .map(rule -> rule.getMaxAmount())
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
        boolean mandatoryApproval = category.getRules().stream().anyMatch(rule -> rule.isRequiresMandatoryApproval());
        boolean mandatoryDocumentRequired = category.getRules().stream().anyMatch(rule -> rule.isMandatoryDocumentRequired());

        return new ExpenseCategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                maxAmount,
                mandatoryApproval,
                mandatoryDocumentRequired);
    }
}
