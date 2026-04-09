package com.eventbudget.repository;

import com.eventbudget.model.approval.ApprovalStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    @Query("""
            select s
            from ApprovalStep s
            where s.assignedTo.userId = :approverId
              and s.workflow.currentStepIndex = s.stepOrder
              and s.workflow.claim.status = com.eventbudget.model.domain.ClaimStatus.PENDING_APPROVAL
              and s.decision = 'PENDING'
            order by s.workflow.claim.createdAt asc
            """)
    List<ApprovalStep> findPendingStepsForApprover(@Param("approverId") Long approverId);
}
