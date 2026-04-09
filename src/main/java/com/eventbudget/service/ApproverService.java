package com.eventbudget.service;

import com.eventbudget.dto.ApprovalDecisionRequest;
import com.eventbudget.dto.BudgetApprovalRequest;
import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.ExpenseClaimResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.exception.BusinessException;
import com.eventbudget.exception.ResourceNotFoundException;
import com.eventbudget.exception.UnauthorizedActionException;
import com.eventbudget.model.approval.ApprovalStep;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetStatus;
import com.eventbudget.model.domain.ClaimStatus;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.user.ApprovingAuthority;
import com.eventbudget.repository.ApprovalStepRepository;
import com.eventbudget.repository.ApprovalWorkflowRepository;
import com.eventbudget.repository.ApprovingAuthorityRepository;
import com.eventbudget.repository.BudgetRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproverService {

    private final ApprovingAuthorityRepository approvingAuthorityRepository;
    private final BudgetRepository budgetRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getPendingBudgets(Long approverId) {
        ApprovingAuthority approver = getApprover(approverId);
        return budgetRepository.findByStatusOrderByCreatedAtAsc(BudgetStatus.PENDING_APPROVAL)
                .stream()
                .filter(budget -> approver.canApprove(budget.getTotalAmount()))
                .map(BudgetResponse::from)
                .toList();
    }

    @Transactional
    public BudgetResponse reviewBudget(Long approverId, Long budgetId, BudgetApprovalRequest request) {
        ApprovingAuthority approver = getApprover(approverId);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (budget.getStatus() != BudgetStatus.PENDING_APPROVAL) {
            throw new BusinessException("Budget is not awaiting approval");
        }
        if (!approver.canApprove(budget.getTotalAmount())) {
            throw new UnauthorizedActionException("Your approval limit is insufficient for this budget");
        }

        if (request.approved()) {
            budget.setStatus(BudgetStatus.APPROVED);
            budget.setApprovedBy(approver);
            notificationService.notify(
                    budget.getEvent().getOrganizer(),
                    "Budget approved",
                    "Budget " + budget.getBudgetId() + " has been approved.");
            auditService.log(
                    "BUDGET",
                    budget.getBudgetId(),
                    "APPROVED",
                    approver,
                    request.comment() != null ? request.comment() : "Budget approved");
        } else {
            budget.setStatus(BudgetStatus.REJECTED);
            notificationService.notify(
                    budget.getEvent().getOrganizer(),
                    "Budget rejected",
                    "Budget " + budget.getBudgetId() + " has been rejected.");
            auditService.log(
                    "BUDGET",
                    budget.getBudgetId(),
                    "REJECTED",
                    approver,
                    request.comment() != null ? request.comment() : "Budget rejected");
        }

        return BudgetResponse.from(budget);
    }

    @Transactional(readOnly = true)
    public List<ExpenseClaimResponse> getPendingClaims(Long approverId) {
        getApprover(approverId);
        return approvalStepRepository.findPendingStepsForApprover(approverId)
                .stream()
                .map(step -> ExpenseClaimResponse.from(step.getWorkflow().getClaim()))
                .toList();
    }

    @Transactional
    public ExpenseClaimResponse reviewClaim(Long approverId, Long claimId, ApprovalDecisionRequest request) {
        ApprovingAuthority approver = getApprover(approverId);
        ApprovalWorkflow workflow = approvalWorkflowRepository.findByClaimClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Approval workflow not found for claim"));
        ApprovalStep currentStep = workflow.getCurrentStep();

        if (currentStep == null || currentStep.isDecided()) {
            throw new BusinessException("This claim is not awaiting a decision");
        }
        if (!currentStep.getAssignedTo().getUserId().equals(approverId)) {
            throw new UnauthorizedActionException("This claim is not assigned to you");
        }

        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!decision.equals("APPROVED") && !decision.equals("REJECTED")) {
            throw new BusinessException("Decision must be either APPROVED or REJECTED");
        }

        ExpenseClaim claim = workflow.getClaim();
        currentStep.resolve(decision, request.comment());

        if (decision.equals("APPROVED")) {
            workflow.advance();
            if (workflow.isComplete()) {
                claim.setStatus(ClaimStatus.APPROVED);
                claim.getBudgetCategory().approveCommitted(claim.getAmount());
                notificationService.notify(
                        claim.getSubmittedBy(),
                        "Expense claim approved",
                        "Claim " + claim.getClaimId() + " has been fully approved.");
            } else {
                ApprovalStep nextStep = workflow.getCurrentStep();
                notificationService.notify(
                        nextStep.getAssignedTo(),
                        "Expense claim awaiting approval",
                        "Claim " + claim.getClaimId() + " requires your approval.");
            }
        } else {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.getBudgetCategory().releaseCommitted(claim.getAmount());
            notificationService.notify(
                    claim.getSubmittedBy(),
                    "Expense claim rejected",
                    "Claim " + claim.getClaimId() + " has been rejected.");
        }

        auditService.log(
                "CLAIM",
                claim.getClaimId(),
                decision,
                approver,
                request.comment() != null ? request.comment() : "Claim " + decision.toLowerCase(Locale.ROOT));

        return ExpenseClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long approverId) {
        getApprover(approverId);
        return notificationService.getNotifications(approverId);
    }

    private ApprovingAuthority getApprover(Long approverId) {
        return approvingAuthorityRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));
    }
}
