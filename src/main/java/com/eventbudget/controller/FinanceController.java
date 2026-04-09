package com.eventbudget.controller;

import com.eventbudget.dto.AuditLogResponse;
import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.dto.WorkflowConfigRequest;
import com.eventbudget.dto.WorkflowConfigResponse;
import com.eventbudget.model.domain.ExportFormat;
import com.eventbudget.security.AppUserPrincipal;
import com.eventbudget.service.FinanceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/workflow-configs")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowConfigResponse createWorkflowConfig(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody WorkflowConfigRequest request) {
        return financeService.saveWorkflowConfig(principal.getUserId(), request);
    }

    @GetMapping("/workflow-configs")
    public List<WorkflowConfigResponse> getWorkflowConfigs(@AuthenticationPrincipal AppUserPrincipal principal) {
        return financeService.getWorkflowConfigs(principal.getUserId());
    }

    @GetMapping("/budgets")
    public List<BudgetResponse> getAllBudgets(@AuthenticationPrincipal AppUserPrincipal principal) {
        return financeService.getAllBudgets(principal.getUserId());
    }

    @PostMapping("/budgets/{budgetId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void closeBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long budgetId) {
        financeService.closeBudget(principal.getUserId(), budgetId);
    }

    @GetMapping("/budgets/{budgetId}/export")
    public ResponseEntity<byte[]> exportBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long budgetId,
            @RequestParam(defaultValue = "CSV") ExportFormat format) {
        byte[] payload = financeService.exportBudget(principal.getUserId(), budgetId, format);
        MediaType mediaType = format == ExportFormat.PDF
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("attachment")
                        .filename("budget-" + budgetId + "." + format.name().toLowerCase())
                        .build()
                        .toString())
                .contentType(mediaType)
                .body(payload);
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> getAuditLogs(@AuthenticationPrincipal AppUserPrincipal principal) {
        return financeService.getAuditLogs(principal.getUserId());
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal AppUserPrincipal principal) {
        return financeService.getMyNotifications(principal.getUserId());
    }
}
