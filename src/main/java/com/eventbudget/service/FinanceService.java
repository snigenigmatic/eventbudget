package com.eventbudget.service;

import com.eventbudget.dto.AuditLogResponse;
import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.dto.WorkflowConfigRequest;
import com.eventbudget.dto.WorkflowConfigResponse;
import com.eventbudget.exception.BusinessException;
import com.eventbudget.exception.ResourceNotFoundException;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetStatus;
import com.eventbudget.model.domain.ExportFormat;
import com.eventbudget.model.user.FinanceAdmin;
import com.eventbudget.repository.AuditLogRepository;
import com.eventbudget.repository.BudgetRepository;
import com.eventbudget.repository.FinanceAdminRepository;
import com.eventbudget.repository.WorkflowConfigRepository;
import com.eventbudget.service.export.ReportExporter;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceAdminRepository financeAdminRepository;
    private final WorkflowConfigRepository workflowConfigRepository;
    private final BudgetRepository budgetRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * ADAPTER PATTERN (Structural) — Spring injects every {@link ReportExporter}
     * bean here. The service programs against the target interface and never
     * references a specific format (PDF/CSV). Adding XLSX/HTML only requires
     * a new adapter bean — no changes in this service.
     */
    private final List<ReportExporter> reportExporters;

    private final Map<ExportFormat, ReportExporter> exportersByFormat =
            new EnumMap<>(ExportFormat.class);

    @PostConstruct
    void indexExporters() {
        for (ReportExporter exporter : reportExporters) {
            exportersByFormat.put(exporter.getFormat(), exporter);
        }
    }

    @Transactional
    public WorkflowConfigResponse saveWorkflowConfig(Long financeAdminId, WorkflowConfigRequest request) {
        FinanceAdmin financeAdmin = getFinanceAdmin(financeAdminId);

        if (request.autoApproveLimit().compareTo(request.singleLevelLimit()) > 0
                || request.singleLevelLimit().compareTo(request.multiLevelThreshold()) > 0) {
            throw new BusinessException("Limits must satisfy autoApprove <= singleLevel <= multiLevel");
        }

        WorkflowConfig config = new WorkflowConfig();
        config.setName(request.name());
        config.setAutoApproveLimit(request.autoApproveLimit());
        config.setSingleLevelLimit(request.singleLevelLimit());
        config.setMultiLevelThreshold(request.multiLevelThreshold());
        config = workflowConfigRepository.save(config);

        auditService.log("WORKFLOW_CONFIG", config.getConfigId(), "CREATED", financeAdmin, "Configured approval thresholds");
        return WorkflowConfigResponse.from(config);
    }

    @Transactional(readOnly = true)
    public List<WorkflowConfigResponse> getWorkflowConfigs(Long financeAdminId) {
        getFinanceAdmin(financeAdminId);
        return workflowConfigRepository.findAll().stream()
                .map(WorkflowConfigResponse::from)
                .toList();
    }

    @Transactional
    public void closeBudget(Long financeAdminId, Long budgetId) {
        FinanceAdmin financeAdmin = getFinanceAdmin(financeAdminId);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (budget.getStatus() != BudgetStatus.APPROVED) {
            throw new BusinessException("Only approved budgets can be closed");
        }
        if (!budget.getEvent().hasConcluded()) {
            throw new BusinessException("The event has not concluded yet");
        }
        if (!budget.hasNoPendingClaims()) {
            throw new BusinessException("Budget has pending expense claims");
        }

        budget.setStatus(BudgetStatus.CLOSED);
        budget.setClosedAt(LocalDateTime.now());
        notificationService.notify(
                budget.getEvent().getOrganizer(),
                "Budget closed",
                "Budget " + budget.getBudgetId() + " has been closed by finance.");
        auditService.log("BUDGET", budget.getBudgetId(), "CLOSED", financeAdmin, "Budget closed");
    }

    @Transactional(readOnly = true)
    public byte[] exportBudget(Long financeAdminId, Long budgetId, ExportFormat format) {
        getFinanceAdmin(financeAdminId);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        ReportExporter exporter = exportersByFormat.get(format);
        if (exporter == null) {
            throw new BusinessException("No exporter registered for format: " + format);
        }
        return exporter.export(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets(Long financeAdminId) {
        getFinanceAdmin(financeAdminId);
        return budgetRepository.findAll().stream()
                .map(BudgetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(Long financeAdminId) {
        getFinanceAdmin(financeAdminId);
        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long financeAdminId) {
        getFinanceAdmin(financeAdminId);
        return notificationService.getNotifications(financeAdminId);
    }

    private FinanceAdmin getFinanceAdmin(Long financeAdminId) {
        return financeAdminRepository.findById(financeAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Finance admin not found"));
    }
}
