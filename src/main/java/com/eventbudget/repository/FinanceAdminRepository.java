package com.eventbudget.repository;

import com.eventbudget.model.user.FinanceAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceAdminRepository extends JpaRepository<FinanceAdmin, Long> {
}
