package com.eventbudget.repository;

import com.eventbudget.model.approval.WorkflowConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, Long> {

    Optional<WorkflowConfig> findTopByOrderByConfigIdDesc();
}
