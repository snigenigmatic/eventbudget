package com.eventbudget.repository;

import com.eventbudget.model.approval.ApprovalWorkflow;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    Optional<ApprovalWorkflow> findByClaimClaimId(Long claimId);
}
