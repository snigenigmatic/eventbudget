package com.eventbudget.repository;

import com.eventbudget.model.domain.BudgetCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    List<BudgetCategory> findByBudgetBudgetId(Long budgetId);
}
