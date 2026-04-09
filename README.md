# Event Budget Planning and Expense Approval System

A Spring Boot REST API for managing college event budgets, expense claims, and multi-level approval workflows. Developing for the course "Object Oriented Analysis and Design - UE23CS351A" at PES University. 

**Team C13 — Section C, PES University**
- C Kaustubh (PES1UG23CS154)
- Chiyedu Vishnu (PES1UG23CS169)
- BB Varun Kumar (PES1UG23CS131)
- Dareddy Devesh Reddy (PES1UG23CS172)

---

## Tech Stack

- Java 21 / Spring Boot 3.5.11
- Spring Data JPA + Hibernate
- Spring Security + JWT
- H2 (dev) / MySQL (prod)
- Maven

## Design Patterns

- **Strategy** — approval routing (single-level, multi-level, auto-approval)
- **Factory** — expense claim and approval workflow creation
- **MVC** — controllers, services, repositories

## User Roles

| Role | Responsibilities |
|---|---|
| Event Organizer | Create budgets, submit expense claims |
| Approving Authority | Review and approve/reject expense claims |
| Finance Admin | Configure workflows, close budgets, export reports |

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
├── config/          # Security and app configuration
├── controller/      # REST endpoints
├── dto/             # Request and response objects
├── exception/       # Custom exceptions and global handler
├── model/           # JPA entities
│   ├── user/        # User hierarchy
│   ├── domain/      # Budget, expense, event entities
│   └── approval/    # Workflow, audit, notification entities
├── repository/      # Spring Data JPA interfaces
├── security/        # JWT provider and auth filter
├── service/         # Business logic
│   ├── strategy/    # Approval strategy implementations
│   └── factory/     # Expense and workflow factories
└── util/            # Report generation utilities
```
