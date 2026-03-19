package com.eventbudget.model.domain;

import com.eventbudget.model.user.ApprovingAuthority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
@Getter
@Setter
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetStatus status = BudgetStatus.DRAFT;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetCategory> categories = new ArrayList<>();

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL)
    private List<ExpenseClaim> expenseClaims = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private ApprovingAuthority approvedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public BigDecimal getTotalAllocated() {
        return categories.stream()
                .map(BudgetCategory::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isAllocationValid() {
        return getTotalAllocated().compareTo(totalAmount) == 0;
    }

    public boolean isClosed() {
        return this.status == BudgetStatus.CLOSED;
    }

    public boolean hasNoPendingClaims() {
        return expenseClaims.stream()
                .noneMatch(c -> c.getStatus() == ClaimStatus.PENDING_APPROVAL
                        || c.getStatus() == ClaimStatus.INFO_REQUESTED);
    }
}