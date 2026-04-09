package com.eventbudget.controller;

import com.eventbudget.dto.ApprovalDecisionRequest;
import com.eventbudget.dto.BudgetApprovalRequest;
import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.ExpenseClaimResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.security.AppUserPrincipal;
import com.eventbudget.service.ApproverService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approver")
@RequiredArgsConstructor
public class ApproverController {

    private final ApproverService approverService;

    @GetMapping("/budgets/pending")
    public List<BudgetResponse> getPendingBudgets(@AuthenticationPrincipal AppUserPrincipal principal) {
        return approverService.getPendingBudgets(principal.getUserId());
    }

    @PostMapping("/budgets/{budgetId}/decision")
    public BudgetResponse reviewBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long budgetId,
            @RequestBody BudgetApprovalRequest request) {
        return approverService.reviewBudget(principal.getUserId(), budgetId, request);
    }

    @GetMapping("/claims/pending")
    public List<ExpenseClaimResponse> getPendingClaims(@AuthenticationPrincipal AppUserPrincipal principal) {
        return approverService.getPendingClaims(principal.getUserId());
    }

    @PostMapping("/claims/{claimId}/decision")
    public ExpenseClaimResponse reviewClaim(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long claimId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return approverService.reviewClaim(principal.getUserId(), claimId, request);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal AppUserPrincipal principal) {
        return approverService.getMyNotifications(principal.getUserId());
    }
}
