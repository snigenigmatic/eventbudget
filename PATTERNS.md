# Design Patterns & Principles — EventBudget (Team C13)

This document maps every GoF design pattern and SOLID principle used in the
project to the exact classes that implement it. The professor can open each
file directly — every pattern class starts with a JavaDoc header naming the
pattern.

---

## 1. MVC — Architectural Pattern (enforced by Spring Boot)

| Layer | Representative classes |
|---|---|
| **Model** | `model/user/User` (+ `EventOrganizer`, `ApprovingAuthority`, `FinanceAdmin`), `model/domain/Budget`, `BudgetCategory`, `ExpenseClaim`, `model/approval/ApprovalWorkflow`, `ApprovalStep`, `WorkflowConfig` |
| **View** | `src/main/resources/static/index.html` + `app.js` + `style.css` (thin SPA that consumes the REST API) |
| **Controller** | `controller/AuthController`, `OrganizerController`, `ApproverController`, `FinanceController`, `CatalogController` |

MVC is listed as the framework-enforced architectural pattern and is therefore
NOT counted toward the four design patterns below.

---

## 2. Design Patterns (4 — covering Creational + Structural + Behavioral)

### 2.1 Factory Pattern — Creational
**File:** `src/main/java/com/eventbudget/service/factory/ApprovalWorkflowFactory.java`

**Intent.** Encapsulate the creation logic of an `ApprovalWorkflow` behind a
single method so callers do not have to know which routing algorithm was used.

**Implementation.**
- `ApprovalWorkflowFactory` receives every `ApprovalRoutingStrategy` bean
  from Spring and indexes them in an
  `EnumMap<ApprovalLevel, ApprovalRoutingStrategy>`.
- `createWorkflow(claim, config, approvers)` asks the `WorkflowConfig` to
  resolve the appropriate `ApprovalLevel` from the claim amount and then
  delegates to the matching strategy.

**Caller.** `OrganizerService.submitExpenseClaim(...)`.

**Benefit.** Adding a new `ApprovalLevel` (e.g. `COMMITTEE_LEVEL`) and a new
strategy bean requires zero changes to this factory or to any caller.

---

### 2.2 Adapter Pattern — Structural
**Files:**
- `src/main/java/com/eventbudget/service/export/ReportExporter.java` — target interface
- `src/main/java/com/eventbudget/service/export/CsvReportExporter.java` — adapter (Budget → CSV bytes)
- `src/main/java/com/eventbudget/service/export/PdfReportExporter.java` — adapter (Budget → PDF bytes)

**Intent.** Convert the internal `Budget` domain aggregate into the byte
stream format expected by external consumers (a spreadsheet program, a PDF
reader) without coupling the caller to either format.

**Implementation.**
- `ReportExporter` is the target interface with `getFormat()` and
  `export(Budget)`.
- `CsvReportExporter` and `PdfReportExporter` are concrete adapters — each
  knows how to translate the same `Budget` object into its respective byte
  format. The PDF adapter, in particular, hides the non-trivial PDF 1.4
  object/xref machinery behind the simple `byte[] export(Budget)` method.
- `FinanceService` holds `Map<ExportFormat, ReportExporter>` built from the
  injected bean list; `exportBudget(...)` looks up the right adapter.

**Benefit.** Adding XLSX/HTML/JSON export is a new adapter bean — no edit to
`FinanceService`, `FinanceController`, or the rest of the codebase.

---

### 2.3 Strategy Pattern — Behavioral
**Files:**
- `src/main/java/com/eventbudget/service/strategy/ApprovalRoutingStrategy.java` — strategy interface
- `src/main/java/com/eventbudget/service/strategy/AutoApprovalStrategy.java` — no approver, instant approval
- `src/main/java/com/eventbudget/service/strategy/SingleLevelApprovalStrategy.java` — one approver step
- `src/main/java/com/eventbudget/service/strategy/MultiLevelApprovalStrategy.java` — two approver steps (Coordinator → Dean)

**Intent.** Define a family of interchangeable algorithms for building an
approval workflow and let the runtime amount pick the correct one.

**Implementation.**
- Each concrete strategy implements `supports(): ApprovalLevel` and
  `createWorkflow(claim, config, approvers): ApprovalWorkflow`.
- Spring discovers every strategy and `ApprovalWorkflowFactory` dispatches
  to the right one.

**Benefit.** The routing decision is isolated in one class per algorithm;
swapping or extending algorithms does not require `if/else` ladders in
callers.

---

### 2.4 Chain of Responsibility — Behavioral
**Files:**
- `src/main/java/com/eventbudget/model/approval/ApprovalWorkflow.java` — chain owner / dispatcher
- `src/main/java/com/eventbudget/model/approval/ApprovalStep.java` — concrete handler
- Driver code: `src/main/java/com/eventbudget/service/ApproverService.java`, method `reviewClaim(...)`

**Intent.** Pass a claim through an ordered chain of approvers; each link
either handles (approves and forwards) or terminates (rejects).

**Implementation.**
- `ApprovalWorkflow.steps` is a `List<ApprovalStep>` ordered by
  `stepOrder`.
- `currentStepIndex` points at the currently responsible handler.
- The active handler calls `ApprovalStep.resolve(decision, comment)`.
  - On `APPROVED`, `ApproverService` calls `workflow.advance()` to forward
    to the next handler.
  - On `REJECTED`, the chain terminates and the claim is marked
    `REJECTED`.
- When `workflow.isComplete()` is `true`, every handler in the chain has
  approved and the claim is finalised.

**Benefit.** Adding a third or fourth approval tier (Registrar, VC, …) is a
matter of building a longer chain in a new `ApprovalRoutingStrategy`; no
caller has to change.

---

## 3. Design Principles (SOLID — 4, one per team member)

### 3.1 Single Responsibility Principle (SRP) — owner: C Kaustubh
- `NotificationService` dispatches notifications only.
- `AuditService` writes audit entries only.
- `AuthService` does authentication only; `JwtService` handles token
  concerns only.
- Each `ReportExporter` handles exactly one format.

Controllers (`AuthController`, `OrganizerController`, `ApproverController`,
`FinanceController`) only deal with HTTP concerns and delegate all business
logic to services.

### 3.2 Open/Closed Principle (OCP) — owner: Chiyedu Vishnu
- `ApprovalRoutingStrategy` is open for extension: register a new
  `@Component` and `ApprovalWorkflowFactory` picks it up automatically.
- `ReportExporter` is open for extension: register a new adapter bean and
  `FinanceService` wires it in at startup via `@PostConstruct`.
- Neither the factory nor `FinanceService` needs to be modified.

### 3.3 Liskov Substitution Principle (LSP) — owner: Dareddy Devesh Reddy
- `User` is the base class; `EventOrganizer`, `ApprovingAuthority`, and
  `FinanceAdmin` are subtypes used interchangeably wherever a `User` is
  expected (e.g. `AuditLog.performedBy`, `Notification.recipient`).
- `ApprovingAuthority.canApprove(amount)` strengthens the contract of
  `User` without violating it.

### 3.4 Dependency Inversion Principle (DIP) — owner: BB Varun Kumar
- Services depend on Spring Data JPA *interfaces* (`BudgetRepository`,
  `ExpenseClaimRepository`, …), never on concrete implementations.
- `ApprovalWorkflowFactory` depends on the `ApprovalRoutingStrategy`
  interface.
- `FinanceService` depends on the `ReportExporter` interface.

---

## 4. Quick Questions the Professor May Ask

| Question | Short answer |
|---|---|
| "Which pattern is used to decide between auto / single / multi-level approval?" | **Strategy** — `ApprovalRoutingStrategy` with three implementations, selected by `ApprovalWorkflowFactory`. |
| "Who creates the `ApprovalWorkflow` object?" | **Factory** — `ApprovalWorkflowFactory.createWorkflow(...)` looks up the right strategy from an `EnumMap<ApprovalLevel, ApprovalRoutingStrategy>`. |
| "How do you export a budget to CSV and PDF with the same calling code?" | **Adapter** — `ReportExporter` target interface adapted by `CsvReportExporter` and `PdfReportExporter`; `FinanceService` picks the adapter from a `Map<ExportFormat, ReportExporter>`. |
| "How does a multi-level claim move from Coordinator to Dean?" | **Chain of Responsibility** — `ApprovalWorkflow` holds an ordered list of `ApprovalStep`s and advances `currentStepIndex` on each approval. |
| "Where is MVC?" | Controllers under `controller/`, services + JPA entities form the Model layer, static SPA under `resources/static/` is the View. |
| "Which principle lets you add a `COMMITTEE_LEVEL` tier without editing existing code?" | **OCP** — drop in a new `ApprovalRoutingStrategy` bean; factory auto-discovers it. |
| "Which principle lets a `FinanceAdmin` be used as a `User` argument?" | **LSP** — subclasses substitute for the base `User`. |
| "Why do services not import repository implementations?" | **DIP** — services depend on repository interfaces (Spring Data JPA abstraction). |
| "Why is each service small and focused?" | **SRP** — one responsibility each. |
