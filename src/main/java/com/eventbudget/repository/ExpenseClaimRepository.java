package com.eventbudget.repository;

import com.eventbudget.model.domain.ExpenseClaim;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, Long> {

    List<ExpenseClaim> findBySubmittedByUserIdOrderByCreatedAtDesc(Long organizerId);

    List<ExpenseClaim> findByBudgetBudgetIdOrderByCreatedAtDesc(Long budgetId);
}
