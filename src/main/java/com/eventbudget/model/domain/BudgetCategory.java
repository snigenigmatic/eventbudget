package com.eventbudget.model.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
@Table(name = "budget_categories")
@Getter
@Setter
public class BudgetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal approvedExpenditure = BigDecimal.ZERO;

    public BigDecimal getAvailableBalance() {
        return allocatedAmount.subtract(committedAmount).subtract(approvedExpenditure);
    }

    public double getUtilizationPercent() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return committedAmount.add(approvedExpenditure)
                .divide(allocatedAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public void commitAmount(BigDecimal amount) {
        this.committedAmount = this.committedAmount.add(amount);
    }

    public void releaseCommitted(BigDecimal amount) {
        this.committedAmount = this.committedAmount.subtract(amount);
    }

    public void approveCommitted(BigDecimal amount) {
        this.committedAmount = this.committedAmount.subtract(amount);
        this.approvedExpenditure = this.approvedExpenditure.add(amount);
    }

    public boolean isNearLimit() {
        return getUtilizationPercent() >= 80.0;
    }
}