
  ---
  Problem Statement

  Event management at PES University involves multiple stakeholders — organizers who plan events, authorities who approve budgets, and a finance office that oversees expenditure. Currently
  this process is manual and error-prone: budget requests are submitted informally, approval routing is inconsistent, and there is no audit trail.

  EventBudget is a web-based Event Budget Planning and Expense Approval System that digitises this workflow. Organizers create events and submit structured budgets; approval is automatically
   routed based on configurable thresholds (auto-approve, single-level, or multi-level); expense claims are tracked against approved budgets; and the Finance Admin can close budgets and
  export financial reports. The system ensures accountability, enforces spending limits, and provides a complete audit trail of every decision.

  ---
  Key Features

  1. Role-Based Access Control — Three distinct roles: Event Organizer, Approving Authority (Faculty Coordinator / Dean), and Finance Admin, each with a dedicated dashboard and restricted
  API endpoints.
  2. Event Management — Organizers create and manage events with name, date, venue, and description.
  3. Budget Planning with Category Allocation — Budgets are broken down into expense categories (Venue, Food, Marketing, Travel). The sum of category allocations must exactly equal the
  declared total.
  4. Configurable Approval Workflow — Finance Admin sets three thresholds: Auto-Approve Limit, Single-Level Limit, and Multi-Level Threshold. Claims are routed automatically based on amount.
  5. Multi-Tier Expense Approval — Auto-approved (≤ ₹2,000), single-level (₹2,001–₹10,000), or multi-level (> ₹10,000) approval chains depending on WorkflowConfig and category rules.
  6. Expense Claim Submission — Organizers submit claims against approved budget categories with vendor, amount, date, and optional supporting documents. Travel and Venue claims require
  mandatory documentation.
  7. Real-Time Budget Utilisation Tracking — Each BudgetCategory tracks committed (pending) and approved expenditure separately, with available balance computed live.
  8. Notification System — In-app notifications are sent to relevant users at every workflow transition (budget approved/rejected, claim approved/rejected).
  9. Audit Trail — Every significant action (budget approval, claim decision, budget close) is recorded with actor, timestamp, and description.
  10. Financial Reporting — Finance Admin can export any budget as a CSV (full category breakdown) or PDF report.

  ---
  Models

  Use Case Diagram

  Actors: Event Organizer, Approving Authority, Finance Admin, System (for auto-approval)

  ┌─────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │        Actor        │                                                             Use Cases                                                             │
  ├─────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Event Organizer     │ Register / Login, Create Event, Create Budget, View Budget Status, Submit Expense Claim, View Claim Status, View Notifications    │
  ├─────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Approving Authority │ Login, View Pending Budgets, Approve/Reject Budget, View Pending Claims, Approve/Reject Claim, View Notifications                 │
  ├─────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Finance Admin       │ Login, Configure Workflow Thresholds, View All Budgets, Close Budget, Export Budget (CSV/PDF), View Audit Log, View Notifications │
  ├─────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ System              │ Auto-Approve Claim (when amount ≤ autoApproveLimit and no mandatory approval)                                                     │
  └─────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

  Include relationships:
  - Submit Expense Claim <<include>> Validate Budget Category Balance
  - Create Budget <<include>> Validate Category Sum Equals Total
  - Approve/Reject Budget <<include>> Send Notification
  - Approve/Reject Claim <<include>> Send Notification, Update Budget Category Balance

  ---
  Class Diagram

  User Hierarchy (Single-Table Inheritance)
  User (abstract)
    - userId: Long
    - name: String
    - email: String
    - passwordHash: String
    - role: UserRole {ORGANIZER, APPROVER, FINANCE_ADMIN}
    - createdAt: LocalDateTime
      ├── EventOrganizer
      │     - department: String
      ├── ApprovingAuthority
      │     - authorizationLimit: BigDecimal
      │     - designation: String
      │     + canApprove(amount): boolean
      └── FinanceAdmin
            - department: String

  Domain Model
  Event
    - eventId, name, description, eventDate, venue
    - organizer: EventOrganizer (ManyToOne)
    - budget: Budget (OneToOne)
    + hasConcluded(): boolean

  Budget
    - budgetId, totalAmount, status: BudgetStatus, createdAt, closedAt
    - event: Event (OneToOne)
    - categories: List<BudgetCategory> (OneToMany)
    - approvedBy: ApprovingAuthority (ManyToOne)
    + isAllocationValid(): boolean
    + hasNoPendingClaims(): boolean

  BudgetCategory
    - categoryId, allocatedAmount, committedAmount, approvedExpenditure
    - budget: Budget (ManyToOne)
    - expenseCategory: ExpenseCategory (ManyToOne)
    + getAvailableBalance(): BigDecimal
    + getUtilizationPercent(): double
    + commitAmount(), releaseCommitted(), approveCommitted()

  ExpenseCategory
    - categoryId, name, maxAmount
    - rules: List<CategoryRule>
    + requiresSupportingDocuments(): boolean
    + requiresMandatoryApproval(): boolean

  ExpenseClaim
    - claimId, vendor, amount, description, expenseDate, status: ClaimStatus
    - budget: Budget (ManyToOne)
    - budgetCategory: BudgetCategory (ManyToOne)
    - submittedBy: EventOrganizer (ManyToOne)
    - documents: List<SupportingDocument>

  SupportingDocument
    - documentId, fileName, fileType, storageUrl
    - claim: ExpenseClaim (ManyToOne)

  Approval Model
  WorkflowConfig
    - configId, name
    - autoApproveLimit, singleLevelLimit, multiLevelThreshold: BigDecimal
    + resolveLevel(amount): ApprovalLevel

  ApprovalWorkflow
    - workflowId
    - claim: ExpenseClaim (OneToOne)
    - steps: List<ApprovalStep> (OneToMany, ordered)
    - currentStepIndex: int
    - config: WorkflowConfig (ManyToOne)
    + getCurrentStep(): ApprovalStep
    + advance(), isComplete(), isMultiLevel()

  ApprovalStep
    - stepId, stepOrder, decision, comment, decidedAt
    - workflow: ApprovalWorkflow (ManyToOne)
    - assignedTo: ApprovingAuthority (ManyToOne)
    + isDecided(): boolean
    + resolve(decision, comment)

  AuditLog
    - logId, entityType, entityId, action, description, timestamp
    - performedBy: User (ManyToOne)

  Notification
    - notificationId, subject, message, sentAt, read
    - recipient: User (ManyToOne)

  ---
  State Diagram

  Budget lifecycle:
  [DRAFT] ──submit──> [PENDING_APPROVAL] ──approve──> [APPROVED] ──close──> [CLOSED]
                                          ──reject──>  [REJECTED]

  ExpenseClaim lifecycle:
  [PENDING_APPROVAL] ──auto-approve──> [AUTO_APPROVED]
  [PENDING_APPROVAL] ──approve (all steps)──> [APPROVED]
  [PENDING_APPROVAL] ──reject (any step)──> [REJECTED]

  Guards:
  - DRAFT → PENDING_APPROVAL: budget.isAllocationValid() == true
  - APPROVED → CLOSED: event.hasConcluded() == true AND budget.hasNoPendingClaims() == true

  ---
  Activity Diagrams

  Activity Diagram 1 — Submit and Approve a Budget:
  1. Organizer creates Event
  2. Organizer creates Budget with category allocations
  3. System validates sum(allocations) == totalAmount → error if not
  4. Budget status set to PENDING_APPROVAL; Approver notified
  5. Approver reviews budget
  6. [Approved] → status = APPROVED; Organizer notified
  7. [Rejected] → status = REJECTED; Organizer notified

  Activity Diagram 2 — Submit and Route an Expense Claim:
  1. Organizer selects approved budget + category
  2. Organizer fills claim (vendor, amount, date, optional documents)
  3. System checks available balance → error if insufficient
  4. System commits amount from BudgetCategory
  5. System calls WorkflowConfig.resolveLevel(amount):
    - amount ≤ autoApproveLimit AND no mandatory approval → AUTO_APPROVED (end)
    - amount ≤ singleLevelLimit → create single ApprovalStep for one Approver
    - amount > multiLevelThreshold → create two ApprovalSteps (Coordinator + Dean)
  6. Assigned Approver reviews claim
  7. [Approved] → if more steps, notify next approver; else APPROVED + update BudgetCategory
  8. [Rejected] → REJECTED + release committed amount from BudgetCategory; Organizer notified

  ---
  Design Principles and Design Patterns

  MVC Architecture — Yes

  ┌──────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────┐
  │            Layer             │                                     Classes                                     │
  ├──────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┤
  │ Model                        │ All classes under model/ package (User hierarchy, Budget, ExpenseClaim, etc.)   │
  ├──────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┤
  │ Controller (View-Controller) │ AuthController, OrganizerController, ApproverController, FinanceController      │
  ├──────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┤
  │ Service                      │ OrganizerService, ApproverService, FinanceService, AuthService (business logic) │
  ├──────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────┤
  │ Repository                   │ Spring Data JPA repositories (data access layer)                                │
  └──────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────┘

  The frontend SPA (app.js) acts as a thin View, consuming the REST API.

  ---
  Design Principles

  1. Single Responsibility Principle (SRP)
  Each service class has exactly one responsibility. OrganizerService handles only organizer operations; NotificationService handles only notification delivery; AuditService handles only
  audit logging. Controllers delegate all business logic to their respective service — they only handle HTTP concerns.

  2. Open/Closed Principle (OCP)
  The approval routing system is open for extension, closed for modification. Adding a new approval tier (e.g., COMMITTEE_LEVEL) requires only implementing a new ApprovalRoutingStrategy and
  registering it — no existing code changes. ApprovalWorkflowFactory auto-discovers strategies via Spring's dependency injection.

  3. Liskov Substitution Principle (LSP)
  User, EventOrganizer, ApprovingAuthority, and FinanceAdmin form an inheritance hierarchy. Any method accepting a User works correctly when given any subtype.
  ApprovingAuthority.canApprove() extends User without breaking its contract.

  4. Dependency Inversion Principle (DIP)
  All services depend on repository interfaces (Spring Data JPA), not concrete implementations. ApprovalWorkflowFactory depends on the ApprovalRoutingStrategy interface, not concrete
  strategy classes.

  ---
  Design Patterns

  1. Strategy Pattern

  - Intent: Define a family of algorithms, encapsulate each one, and make them interchangeable.
  - Where used: Approval routing in com.eventbudget.service.strategy
  - Interface: ApprovalRoutingStrategy — defines supports(): ApprovalLevel and createWorkflow(...): ApprovalWorkflow
  - Concrete Strategies:
    - AutoApprovalStrategy — instantly approves the claim, no steps created
    - SingleLevelApprovalStrategy — creates one ApprovalStep assigned to the most capable approver
    - MultiLevelApprovalStrategy — creates two sequential ApprovalSteps (Coordinator, then Dean)
  - Context: ApprovalWorkflowFactory selects the strategy based on WorkflowConfig.resolveLevel(amount)
  - Benefit: Adding a new approval tier requires no changes to existing strategies or the factory dispatch logic.

  2. Factory Pattern

  - Intent: Encapsulate object creation logic behind a single interface.
  - Where used: ApprovalWorkflowFactory in com.eventbudget.service.factory
  - How it works: The factory receives all ApprovalRoutingStrategy beans from Spring, stores them in an EnumMap<ApprovalLevel, ApprovalRoutingStrategy>, and exposes a single
  createWorkflow(claim, config, approvers) method. Callers do not need to know which strategy is used — the factory resolves the correct one from the amount.
  - Benefit: Decouples the OrganizerService (which triggers claim creation) from the approval routing logic entirely.

  ---
  GitHub Link

  (Make the repository public and paste the URL here)
  https://github.com/snigenigmatic/eventbudget  ← verify this is correct and public

  ---
  Screenshots Needed

  Take these in order — they cover every role and feature:

  ┌─────┬──────────────────────────────────────────┬────────────────────────────────────┐
  │  #  │               What to show               │          How to get there          │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 1   │ Login page                               │ Open http://localhost:8080         │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 2   │ Organizer Dashboard (stat cards)         │ Login as organizer                 │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 3   │ Create Event modal                       │ Organizer → Events → New Event     │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 4   │ Events table                             │ After creating event               │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 5   │ Create Budget modal (with category rows) │ Organizer → Budgets → New Budget   │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 6   │ Approver Dashboard                       │ Login as coordinator               │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 7   │ Pending Budgets table                    │ Approver → Pending Budgets         │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 8   │ Approve Budget modal                     │ Click Approve on a budget          │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 9   │ Submit Claim modal                       │ Organizer → Claims → Submit Claim  │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 10  │ Pending Claims (approver view)           │ Approver → Pending Claims          │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 11  │ Finance Dashboard                        │ Login as finance@eventbudget.local │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 12  │ Workflow Config table + New Config modal │ Finance → Workflow Config          │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 13  │ All Budgets with Export buttons          │ Finance → All Budgets              │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 14  │ Audit Log table                          │ Finance → Audit Log                │
  ├─────┼──────────────────────────────────────────┼────────────────────────────────────┤
  │ 15  │ Downloaded PDF report                    │ Click PDF on an approved budget    │
  └─────┴──────────────────────────────────────────┴────────────────────────────────────┘

  ---
  Individual Contributions

  Fill this in based on your team split. Typical breakdown for a 3-member team:

  ┌──────────┬─────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │   Name   │ SRN │                                                             Module Worked On                                                              │
  ├──────────┼─────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Member 1 │     │ Domain model design, JPA entities (User hierarchy, Budget, Event, ExpenseClaim), DataInitializer, Spring Security / JWT                   │
  ├──────────┼─────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Member 2 │     │ Service layer (OrganizerService, ApproverService), Strategy Pattern implementation, ApprovalWorkflowFactory, unit & integration tests     │
  ├──────────┼─────┼───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Member 3 │     │ FinanceService, ReportExportUtil (CSV/PDF), Frontend SPA (index.html, app.js, style.css), REST controllers, Notification & Audit services │
  └──────────┴─────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

