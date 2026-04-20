# Event Budget Planning and Expense Approval System

A Spring Boot REST API for managing college event budgets, expense claims, and multi-level approval workflows. Developed for the course **UE23CS352B – Object Oriented Analysis and Design** at PES University.

**Team C13 — Section C, PES University**
- C Kaustubh (PES1UG23CS154)
- Chiyedu Vishnu (PES1UG23CS169)
- Dareddy Devesh Reddy (PES1UG23CS171)
- BB Varun Kumar (PES1UG23CS131)

---

## Tech Stack

- Java 21 / Spring Boot 3.5.11
- Spring Data JPA + Hibernate
- Spring Security + JWT
- H2 (dev) / MySQL (prod)
- Maven

---

## Architecture & Design — at a glance

**MVC Architecture** (framework-enforced, not counted toward the 4 patterns)

| Layer | Package | Classes |
|---|---|---|
| Model (domain + persistence) | `com.eventbudget.model.*` | `User`, `Budget`, `BudgetCategory`, `ExpenseClaim`, `ApprovalWorkflow`, … |
| View | `src/main/resources/static/` | `index.html`, `app.js`, `style.css` (SPA consuming REST) |
| Controller | `com.eventbudget.controller.*` | `AuthController`, `OrganizerController`, `ApproverController`, `FinanceController` |
| Service (business logic) | `com.eventbudget.service.*` | `OrganizerService`, `ApproverService`, `FinanceService`, `AuthService`, `NotificationService`, `AuditService` |
| Repository (data access) | `com.eventbudget.repository.*` | Spring Data JPA repositories |

### Design Principles (SOLID — 1 per member)

| # | Principle | Where it is applied |
|---|---|---|
| 1 | **Single Responsibility** | Each service has exactly one responsibility — `NotificationService` only dispatches notifications, `AuditService` only writes audit logs, `ReportExporter` implementations only serialise a budget. |
| 2 | **Open / Closed** | `ApprovalRoutingStrategy` and `ReportExporter` are both open for extension (add a bean) and closed for modification (callers are untouched). |
| 3 | **Liskov Substitution** | `EventOrganizer`, `ApprovingAuthority`, `FinanceAdmin` all substitute `User`; `canApprove` extends the base contract without breaking it. |
| 4 | **Dependency Inversion** | All services depend on repository *interfaces* (Spring Data JPA) and on the `ApprovalRoutingStrategy` / `ReportExporter` interfaces rather than concrete classes. |

### Design Patterns (4 — covering Creational + Structural + Behavioral)

| # | Category | Pattern | Classes |
|---|---|---|---|
| 1 | Creational | **Factory** | [`ApprovalWorkflowFactory`](src/main/java/com/eventbudget/service/factory/ApprovalWorkflowFactory.java) |
| 2 | Structural | **Adapter** | [`ReportExporter`](src/main/java/com/eventbudget/service/export/ReportExporter.java), [`CsvReportExporter`](src/main/java/com/eventbudget/service/export/CsvReportExporter.java), [`PdfReportExporter`](src/main/java/com/eventbudget/service/export/PdfReportExporter.java) |
| 3 | Behavioral | **Strategy** | [`ApprovalRoutingStrategy`](src/main/java/com/eventbudget/service/strategy/ApprovalRoutingStrategy.java) + `AutoApprovalStrategy`, `SingleLevelApprovalStrategy`, `MultiLevelApprovalStrategy` |
| 4 | Behavioral | **Chain of Responsibility** | [`ApprovalWorkflow`](src/main/java/com/eventbudget/model/approval/ApprovalWorkflow.java) + ordered [`ApprovalStep`](src/main/java/com/eventbudget/model/approval/ApprovalStep.java) handlers |

See [`PATTERNS.md`](PATTERNS.md) for full explanations with file references.

---

## User Roles

| Role | Responsibilities |
|---|---|
| Event Organizer | Create events & budgets, submit expense claims |
| Approving Authority | Review and approve/reject budgets and claims |
| Finance Admin | Configure workflow thresholds, close budgets, export reports |

---

## Individual Contributions (major + minor use case per member)

| Member | Major use case owned | Minor use case owned |
|---|---|---|
| C Kaustubh | Register / Login with JWT auth (AuthController, AuthService, JwtService, JwtAuthenticationFilter, SecurityConfig) | Role-based access & user profile (`/api/auth/me`) |
| Chiyedu Vishnu | Create Event + Budget with category allocation (OrganizerController, OrganizerService, Budget/BudgetCategory entities) | View own budgets / events / notifications |
| Dareddy Devesh Reddy | Multi-level Expense Claim approval (ApproverService + Strategy/Factory/Chain-of-Responsibility) | Approve / Reject Budget |
| BB Varun Kumar | Submit Expense Claim + Export Budget CSV / PDF (FinanceService, ReportExporter adapters) | Configure Workflow thresholds, View Audit Log |

---

## Running Locally
```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`
H2 console at `http://localhost:8080/h2-console`

## Seeded Users

All seeded accounts use password `password123`.

| Role | Email |
|---|---|
| Organizer | `organizer@eventbudget.local` |
| Approver | `coordinator@eventbudget.local` |
| Approver | `dean@eventbudget.local` |
| Finance Admin | `finance@eventbudget.local` |

## Key API Endpoints

- `POST /api/auth/register` — register a new organizer
- `POST /api/auth/login` — get a JWT token
- `GET /api/auth/me` — get the authenticated user profile
- `GET /api/catalog/expense-categories` — list available expense categories and rules
- `POST /api/organizer/events` — create an event
- `POST /api/organizer/budgets` — submit a budget for approval
- `POST /api/organizer/claims` — submit an expense claim
- `GET /api/approver/budgets/pending` — list pending budgets
- `POST /api/approver/budgets/{budgetId}/decision` — approve or reject a budget
- `GET /api/approver/claims/pending` — list pending expense claims
- `POST /api/approver/claims/{claimId}/decision` — approve or reject a claim
- `POST /api/finance/workflow-configs` — create approval thresholds
- `POST /api/finance/budgets/{budgetId}/close` — close an approved budget
- `GET /api/finance/budgets/{budgetId}/export?format=CSV|PDF` — export a budget report

## Project Structure
```
src/main/java/com/eventbudget/
├── config/              # Security and app configuration
├── controller/          # REST endpoints (MVC Controllers)
├── dto/                 # Request and response objects
├── exception/           # Custom exceptions and global handler
├── model/               # JPA entities (MVC Model)
│   ├── user/            # User hierarchy (LSP)
│   ├── domain/          # Budget, expense, event entities
│   └── approval/        # ApprovalWorkflow + ApprovalStep
│                        #   ↳ Chain of Responsibility (Behavioral)
├── repository/          # Spring Data JPA interfaces (DIP)
├── security/            # JWT provider and auth filter
└── service/             # Business logic
    ├── strategy/        # Strategy Pattern (Behavioral)
    ├── factory/         # Factory Pattern (Creational)
    └── export/          # Adapter Pattern (Structural)
```
