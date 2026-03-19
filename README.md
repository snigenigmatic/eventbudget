# Event Budget Planning and Expense Approval System

A Spring Boot REST API for managing college event budgets, expense claims, and multi-level approval workflows.

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