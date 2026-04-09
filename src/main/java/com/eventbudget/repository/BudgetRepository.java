package com.eventbudget.repository;

import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByEventEventId(Long eventId);

    List<Budget> findByEventOrganizerUserIdOrderByCreatedAtDesc(Long organizerId);

    List<Budget> findByStatusOrderByCreatedAtAsc(BudgetStatus status);
}
