package com.eventbudget.service;

import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.CreateBudgetRequest;
import com.eventbudget.dto.CreateEventRequest;
import com.eventbudget.dto.CreateExpenseClaimRequest;
import com.eventbudget.dto.EventResponse;
import com.eventbudget.dto.ExpenseClaimResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.dto.SupportingDocumentRequest;
import com.eventbudget.exception.BusinessException;
import com.eventbudget.exception.ResourceNotFoundException;
import com.eventbudget.exception.UnauthorizedActionException;
import com.eventbudget.model.approval.ApprovalWorkflow;
import com.eventbudget.model.approval.ApprovalLevel;
import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.Budget;
import com.eventbudget.model.domain.BudgetCategory;
import com.eventbudget.model.domain.BudgetStatus;
import com.eventbudget.model.domain.CategoryRule;
import com.eventbudget.model.domain.ClaimStatus;
import com.eventbudget.model.domain.Event;
import com.eventbudget.model.domain.ExpenseCategory;
import com.eventbudget.model.domain.ExpenseClaim;
import com.eventbudget.model.domain.SupportingDocument;
import com.eventbudget.model.user.ApprovingAuthority;
import com.eventbudget.model.user.EventOrganizer;
import com.eventbudget.repository.ApprovingAuthorityRepository;
import com.eventbudget.repository.BudgetCategoryRepository;
import com.eventbudget.repository.BudgetRepository;
import com.eventbudget.repository.EventOrganizerRepository;
import com.eventbudget.repository.EventRepository;
import com.eventbudget.repository.ExpenseCategoryRepository;
import com.eventbudget.repository.ExpenseClaimRepository;
import com.eventbudget.repository.WorkflowConfigRepository;
import com.eventbudget.service.factory.ApprovalWorkflowFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final EventOrganizerRepository eventOrganizerRepository;
    private final EventRepository eventRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final WorkflowConfigRepository workflowConfigRepository;
    private final ApprovingAuthorityRepository approvingAuthorityRepository;
    private final ApprovalWorkflowFactory approvalWorkflowFactory;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public EventResponse createEvent(Long organizerId, CreateEventRequest request) {
        EventOrganizer organizer = getOrganizer(organizerId);

        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setVenue(request.venue());
        event.setOrganizer(organizer);

        event = eventRepository.save(event);
        auditService.log("EVENT", event.getEventId(), "CREATED", organizer, "Created event " + event.getName());
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long organizerId) {
        return eventRepository.findByOrganizerUserIdOrderByEventDateDesc(organizerId)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional
    public BudgetResponse createBudget(Long organizerId, CreateBudgetRequest request) {
        EventOrganizer organizer = getOrganizer(organizerId);
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!event.getOrganizer().getUserId().equals(organizerId)) {
            throw new UnauthorizedActionException("You can only create budgets for your own events");
        }
        if (event.getBudget() != null || budgetRepository.findByEventEventId(event.getEventId()).isPresent()) {
            throw new BusinessException("A budget already exists for this event");
        }

        Budget budget = new Budget();
        budget.setEvent(event);
        budget.setTotalAmount(request.totalAmount());
        budget.setStatus(BudgetStatus.PENDING_APPROVAL);

        BigDecimal allocated = BigDecimal.ZERO;
        for (var allocation : request.categories()) {
            ExpenseCategory expenseCategory = expenseCategoryRepository.findById(allocation.expenseCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense category not found: " + allocation.expenseCategoryId()));

            BudgetCategory category = new BudgetCategory();
            category.setBudget(budget);
            category.setExpenseCategory(expenseCategory);
            category.setAllocatedAmount(allocation.allocatedAmount());
            budget.getCategories().add(category);
            allocated = allocated.add(allocation.allocatedAmount());
        }

        if (allocated.compareTo(request.totalAmount()) != 0) {
            throw new BusinessException("Budget allocations must equal the total budget amount");
        }

        event.setBudget(budget);
        Budget savedBudget = budgetRepository.save(budget);

        List<ApprovingAuthority> approvers = approvingAuthorityRepository.findAllByOrderByAuthorizationLimitAsc();
        approvers.forEach(approver -> notificationService.notify(
                approver,
                "Budget awaiting approval",
                "Budget " + savedBudget.getBudgetId() + " for event " + event.getName() + " is awaiting approval."));

        auditService.log("BUDGET", savedBudget.getBudgetId(), "CREATED", organizer, "Submitted budget for event " + event.getName());
        return BudgetResponse.from(savedBudget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getMyBudgets(Long organizerId) {
        return budgetRepository.findByEventOrganizerUserIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(BudgetResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseClaimResponse submitExpenseClaim(Long organizerId, CreateExpenseClaimRequest request) {
        EventOrganizer organizer = getOrganizer(organizerId);
        Budget budget = budgetRepository.findById(request.budgetId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));

        if (!budget.getEvent().getOrganizer().getUserId().equals(organizerId)) {
            throw new UnauthorizedActionException("You can only submit claims against your own budgets");
        }
        if (budget.getStatus() != BudgetStatus.APPROVED) {
            throw new BusinessException("Expense claims can only be submitted against approved budgets");
        }

        BudgetCategory budgetCategory = budgetCategoryRepository.findById(request.budgetCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget category not found"));
        if (!budgetCategory.getBudget().getBudgetId().equals(budget.getBudgetId())) {
            throw new BusinessException("Budget category does not belong to the provided budget");
        }
        if (request.expenseDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Expense date cannot be in the future");
        }
        if (budgetCategory.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException("Insufficient available balance in the selected budget category");
        }

        boolean mandatoryApproval = validateRules(budgetCategory, request.amount(), request.documents());

        ExpenseClaim claim = new ExpenseClaim();
        claim.setBudget(budget);
        claim.setBudgetCategory(budgetCategory);
        claim.setSubmittedBy(organizer);
        claim.setVendor(request.vendor());
        claim.setAmount(request.amount());
        claim.setDescription(request.description());
        claim.setExpenseDate(request.expenseDate());
        claim.setStatus(ClaimStatus.PENDING_APPROVAL);

        attachDocuments(claim, request.documents());

        budgetCategory.commitAmount(request.amount());

        WorkflowConfig workflowConfig = workflowConfigRepository.findTopByOrderByConfigIdDesc()
                .orElseThrow(() -> new BusinessException("No workflow configuration has been defined"));
        ApprovalLevel approvalLevel = workflowConfig.resolveLevel(request.amount());
        if (mandatoryApproval && approvalLevel == ApprovalLevel.AUTO_APPROVED) {
            approvalLevel = ApprovalLevel.SINGLE_LEVEL;
        }

        ApprovalWorkflow workflow = approvalWorkflowFactory.createWorkflow(
                claim,
                workflowConfig,
                approvingAuthorityRepository.findAllByOrderByAuthorizationLimitAsc(),
                approvalLevel);
        claim.setWorkflow(workflow);

        if (workflow.getSteps().isEmpty()) {
            claim.setStatus(ClaimStatus.APPROVED);
            budgetCategory.approveCommitted(request.amount());
            notificationService.notify(
                    organizer,
                    "Expense claim auto-approved",
                    "Claim for " + request.amount() + " was auto-approved.");
        } else {
            notificationService.notify(
                    workflow.getCurrentStep().getAssignedTo(),
                    "Expense claim awaiting approval",
                    "Claim for " + request.amount() + " requires your approval.");
        }

        claim = expenseClaimRepository.save(claim);
        auditService.log("CLAIM", claim.getClaimId(), "SUBMITTED", organizer, "Submitted expense claim for " + request.amount());
        return ExpenseClaimResponse.from(claim);
    }

    @Transactional(readOnly = true)
    public List<ExpenseClaimResponse> getMyClaims(Long organizerId) {
        return expenseClaimRepository.findBySubmittedByUserIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(ExpenseClaimResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long organizerId) {
        return notificationService.getNotifications(organizerId);
    }

    private EventOrganizer getOrganizer(Long organizerId) {
        return eventOrganizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found"));
    }

    private boolean validateRules(
            BudgetCategory budgetCategory,
            BigDecimal amount,
            List<SupportingDocumentRequest> documents) {
        boolean mandatoryApproval = false;
        for (CategoryRule rule : budgetCategory.getExpenseCategory().getApplicableRules()) {
            if (!rule.validate(amount)) {
                throw new BusinessException(rule.getViolationMessage());
            }
            if (rule.isMandatoryDocumentRequired() && (documents == null || documents.isEmpty())) {
                throw new BusinessException("Supporting documents are required for this expense category");
            }
            mandatoryApproval = mandatoryApproval || rule.isRequiresMandatoryApproval();
        }
        return mandatoryApproval;
    }

    private void attachDocuments(ExpenseClaim claim, List<SupportingDocumentRequest> documents) {
        if (documents == null) {
            return;
        }
        for (SupportingDocumentRequest documentRequest : documents) {
            SupportingDocument document = new SupportingDocument();
            document.setClaim(claim);
            document.setFileName(documentRequest.fileName());
            document.setFileType(documentRequest.fileType());
            document.setStorageUrl(documentRequest.storageUrl());
            claim.getDocuments().add(document);
        }
    }
}
