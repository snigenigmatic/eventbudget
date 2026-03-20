package com.eventbudget.model.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "category_rules")
@Getter
@Setter
public class CategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxAmount;

    private boolean requiresMandatoryApproval;

    private boolean isMandatoryDocumentRequired;

    private String description;

    public boolean validate(BigDecimal amount) {
        if (maxAmount == null) return true;
        return amount.compareTo(maxAmount) <= 0;
    }

    public String getViolationMessage() {
        return "Expense exceeds category rule limit of " + maxAmount;
    }
}