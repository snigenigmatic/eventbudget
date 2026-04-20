# Presentation Guide — EventBudget (Team C13)

> **Audience:** Prof. Shridevi Sawant (OOAD, UE23CS352B)
> **Format:** Team-based presentation; each member evaluated individually.
> **Duration target:** 10–12 minutes total presentation + ~5 minutes Q&A.
> **Evaluation focus:** Code structure, MVC, design principles, design patterns, individual ownership.

---

## 1. Slide / Talk Track Outline (12 min)

| # | Slide / Section | Owner | Time | Key points to say |
|---|---|---|---|---|
| 1 | Title + Team | C Kaustubh | 0:30 | Course, project, 4 names + SRNs |
| 2 | Problem Statement | Chiyedu Vishnu | 1:00 | Manual PES event-budget workflow → no audit trail → our system digitises it end-to-end |
| 3 | Architecture Overview (MVC) | C Kaustubh | 1:30 | Show the layer table; highlight `controller/` + `service/` + `repository/` + static SPA |
| 4 | Use Case Diagram walkthrough | Chiyedu Vishnu | 1:00 | 3 actors + System; pick Submit Expense Claim as a "deep-dive" use case |
| 5 | Class Diagram walkthrough | C Kaustubh | 1:00 | `User` hierarchy (for LSP), `Budget`→`BudgetCategory`→`ExpenseClaim` aggregate, `ApprovalWorkflow`→`ApprovalStep` |
| 6 | Design Principles (4) | 1 each | 2:00 | SRP / OCP / LSP / DIP — one per member, 25 seconds each |
| 7 | Design Patterns (4) | 1 each | 3:00 | Factory / Adapter / Strategy / Chain of Responsibility — one per member, 45 seconds each (open the file live if possible) |
| 8 | Live Demo (optional, short) | BB Varun Kumar | 1:30 | Create event → submit budget → approve → submit claim → finance export PDF |
| 9 | Individual Contributions | All | 0:30 | Point at the table in README |
| 10 | Q&A | All | 5:00 | Use the prepared Q&A list below |

---

## 2. Who Says What (Hard Assignments)

Every rubric item has one owner so the professor hears a clear voice-per-topic.

| Team Member | Principle owned | Pattern owned | Code they will "open" live |
|---|---|---|---|
| **C Kaustubh** | Single Responsibility (SRP) | *No GoF pattern owned — owns MVC + JWT security deep dive* | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` |
| **Chiyedu Vishnu** | Open / Closed (OCP) | Factory Pattern | `ApprovalWorkflowFactory`, `OrganizerService.submitExpenseClaim` |
| **Dareddy Devesh Reddy** | Liskov Substitution (LSP) | Strategy + Chain of Responsibility | `ApprovalRoutingStrategy` + 3 impls; `ApprovalWorkflow`, `ApprovalStep`, `ApproverService.reviewClaim` |
| **BB Varun Kumar** | Dependency Inversion (DIP) | Adapter Pattern | `ReportExporter`, `CsvReportExporter`, `PdfReportExporter`, `FinanceService.exportBudget` |

> **Rubric maths check:** 4 principles + 4 patterns across 4 members — one each, exactly as the note on the rubric says.

---

## 3. Per-Member 45-Second Script for Their Pattern

### C Kaustubh — MVC + Security (no GoF pattern, but deep MVC story)

> "Our architecture is MVC, enforced by Spring. The **Model** layer is every class under `model/`, including the `User` inheritance hierarchy, `Budget`, `BudgetCategory`, `ExpenseClaim`, and the `ApprovalWorkflow` aggregate. The **View** is the static SPA under `resources/static/` — `index.html`, `app.js`, `style.css` — which speaks REST to the controllers. The **Controllers** are under `controller/`: `AuthController`, `OrganizerController`, `ApproverController`, `FinanceController`. Every controller does only HTTP concerns and delegates business logic to its corresponding service — that's also where my Single Responsibility Principle story lives. Authentication is JWT, implemented in `JwtAuthenticationFilter` which extends `OncePerRequestFilter` — this is the framework's Template Method pattern, which is why MVC is 'framework-enforced' and not counted among our four GoF patterns."

### Chiyedu Vishnu — Factory Pattern (Creational)

> "I own the Factory Pattern, in `com.eventbudget.service.factory.ApprovalWorkflowFactory`. Its job is to hide the decision of *which* `ApprovalWorkflow` to build from the caller. When an organizer submits a claim, `OrganizerService` just calls `factory.createWorkflow(claim, config, approvers)`. Inside the factory, Spring has already injected every `ApprovalRoutingStrategy` bean into the constructor; I index them in an `EnumMap<ApprovalLevel, ApprovalRoutingStrategy>`. At request time, I ask the `WorkflowConfig` to resolve the level from the claim amount and delegate to the matching strategy. Adding a new `COMMITTEE_LEVEL` is zero change here — that's also my Open/Closed Principle example."

### Dareddy Devesh Reddy — Strategy + Chain of Responsibility (Behavioral)

> "I own two behavioral patterns. The first, **Strategy**, is in `service.strategy.ApprovalRoutingStrategy`. We have three concrete strategies: `AutoApprovalStrategy` creates an empty workflow for auto-approve; `SingleLevelApprovalStrategy` creates one `ApprovalStep` assigned to the most capable approver; `MultiLevelApprovalStrategy` creates a sequence of two steps — Coordinator then Dean. They're interchangeable because they all conform to the same interface, and the Factory picks one at runtime."
>
> "The second pattern, **Chain of Responsibility**, is the relationship between `ApprovalWorkflow` and `ApprovalStep`. Each workflow holds an ordered list of steps; `currentStepIndex` points at the active handler. When the handler calls `resolve(APPROVED, …)`, `ApproverService.reviewClaim` calls `workflow.advance()` to forward the request down the chain. On `REJECTED` the chain terminates immediately. This is how a multi-level claim walks from Coordinator to Dean without the caller knowing who the next approver is."

### BB Varun Kumar — Adapter Pattern (Structural)

> "I own the Adapter Pattern, in `service.export`. The target interface is `ReportExporter`, with `getFormat()` and `byte[] export(Budget)`. Two concrete adapters, `CsvReportExporter` and `PdfReportExporter`, each translate the same `Budget` domain aggregate into a very different external format — CSV bytes or a PDF 1.4 object stream. The PDF adapter is the clearest example of the pattern because PDF requires a header, a catalog, a page tree, a font dictionary, a cross-reference table, and a trailer — a completely different 'interface' than our Java model. My `FinanceService` depends only on the `ReportExporter` interface — that's also my Dependency Inversion example — and picks the correct adapter from an `EnumMap<ExportFormat, ReportExporter>` built in `@PostConstruct`. Adding XLSX is a new bean, no code change elsewhere."

---

## 4. 15-Question Q&A Cheat Sheet

These are the highest-probability professor questions. Each has a **short one-sentence answer** followed by the **file to open** if they want to see it.

### Pattern-specific

1. **Q: Where exactly is the Strategy pattern implemented?**
   **A:** `src/main/java/com/eventbudget/service/strategy/` — one interface, three concrete strategies picked by amount.

2. **Q: Why Strategy and not just `if/else` on the amount?**
   **A:** Strategy makes each algorithm a class, so adding a new tier does not touch `OrganizerService` or `ApprovalWorkflowFactory`.

3. **Q: How does the Factory know which Strategy to use?**
   **A:** `WorkflowConfig.resolveLevel(amount)` returns an `ApprovalLevel` enum; the factory looks up the strategy in an `EnumMap`.

4. **Q: Is your Factory a Simple Factory, Factory Method, or Abstract Factory?**
   **A:** It's a parameterised Factory (Simple Factory variant) — one method with a parameter that selects the product.

5. **Q: Why is the CSV/PDF exporter an Adapter and not another Strategy?**
   **A:** Strategy picks between interchangeable *algorithms for the same task*; Adapter *converts one interface to another*. PDF has a completely different representation (object table + xref + trailer) than our Java `Budget` — that's the "incompatible interface" that Adapter exists to bridge.

6. **Q: Show me the Chain of Responsibility.**
   **A:** Open `ApprovalWorkflow.java` — `List<ApprovalStep> steps` + `currentStepIndex` + `advance()`. The chain is built by `MultiLevelApprovalStrategy` with two ordered steps (Coordinator → Dean).

7. **Q: What happens if the first handler rejects?**
   **A:** `ApproverService.reviewClaim` sets `claim.setStatus(REJECTED)` and releases the committed amount; the chain terminates without calling `advance()`.

### Principle-specific

8. **Q: Give me a SRP example.**
   **A:** `NotificationService` only persists notifications (one `save()`); `AuditService` only writes audit logs; each `ReportExporter` only serialises one format.

9. **Q: Give me an OCP example.**
   **A:** Adding a `COMMITTEE_LEVEL` tier: create a new `ApprovalRoutingStrategy` bean; factory auto-discovers it via Spring DI; zero edits to existing code.

10. **Q: Give me an LSP example.**
    **A:** `AuditLog.performedBy: User` and `Notification.recipient: User` accept `EventOrganizer`, `ApprovingAuthority`, or `FinanceAdmin` interchangeably; `ApprovingAuthority.canApprove(amount)` strengthens (does not weaken) the base contract.

11. **Q: Give me a DIP example.**
    **A:** `FinanceService` depends on `List<ReportExporter>`, not `PdfReportExporter`; every service depends on Spring Data JPA *interfaces*, not concrete repositories.

### Architecture / MVC

12. **Q: Where is the View?**
    **A:** `src/main/resources/static/index.html` + `app.js` + `style.css` — a thin SPA consuming the REST controllers.

13. **Q: If the frontend is the View, what is the Controller?**
    **A:** The REST controllers under `controller/`. They map HTTP routes to service calls — they are the "Controller" in MVC and the View's delegate.

14. **Q: Why are services separated from controllers?**
    **A:** Separation of concerns (SRP) — controllers deal with HTTP; services hold transactional business logic; repositories deal with persistence.

### Persistence / Misc

15. **Q: How is data persisted?**
    **A:** Spring Data JPA with Hibernate; H2 in dev and MySQL in prod (see `pom.xml` + `application.properties`). Every `@Entity` maps to a table; inheritance is single-table on `User`.

### Bonus trick questions you might get

- **"Why is the approval chain inside an entity? Isn't that mixing concerns?"** — It's the aggregate boundary. The workflow's state *is* domain state (which approver is next, has it been approved). Service-layer logic (`ApproverService.reviewClaim`) drives the chain via `advance()` — the entity only holds state + provides navigation.
- **"Why inject a `List<ReportExporter>` instead of a `Map`?"** — Because Spring would key the Map by bean name, not by `ExportFormat`. We index manually in `@PostConstruct` so the key is the domain enum.
- **"Where is Dependency Injection itself used?"** — Every `@Service`, `@Component`, `@RestController` uses constructor injection via Lombok's `@RequiredArgsConstructor`.

---

## 5. Live-Code Walkthrough Order (if professor asks "show me the code")

Do this walk in exactly this order — it tells the story of a single expense claim traveling through every pattern and principle:

1. **Open `OrganizerController.submitClaim`** — shows MVC Controller layer is thin.
2. **Jump into `OrganizerService.submitExpenseClaim`** — shows DIP (depends on 10 repository *interfaces*), SRP (this service only handles organizer workflows), and calls the Factory.
3. **Open `ApprovalWorkflowFactory.createWorkflow`** — show the `EnumMap<ApprovalLevel, ApprovalRoutingStrategy>`. This is the **Factory** pattern.
4. **Open `MultiLevelApprovalStrategy`** — show it creates two ordered `ApprovalStep`s. This is the **Strategy** pattern.
5. **Open `ApprovalWorkflow` and `ApprovalStep`** — show `advance()`, `currentStepIndex`, `resolve()`. This is the **Chain of Responsibility**.
6. **Open `ApproverService.reviewClaim`** — show the chain being driven by `workflow.advance()` on approve and `claim.setStatus(REJECTED)` on reject.
7. **Open `FinanceService.exportBudget`** + `ReportExporter` + `PdfReportExporter`. This is the **Adapter** pattern.
8. Close with **`PATTERNS.md`** open, pointing at the summary table.

Total runtime: ~4 minutes of click-through.

---

## 6. Final Pre-Demo Checklist

- [ ] Repo is **public** on GitHub (the PDF report links it).
- [ ] `mvn spring-boot:run` starts cleanly.
- [ ] All 4 seeded users log in: `organizer@`, `coordinator@`, `dean@`, `finance@eventbudget.local` / `password123`.
- [ ] A budget can be created, approved, have a claim submitted, be approved at multi-level, and exported to CSV **and** PDF.
- [ ] Every team member can **open the file** they own in under 10 seconds (practice this!).
- [ ] `README.md` + `PATTERNS.md` both visible on GitHub page.
- [ ] White-background screenshots refreshed in the report (if dark-mode ones were used).
- [ ] Report PDF has **4 Activity** + **4 State** diagrams and **all 4 Design Patterns** sections.

---

## 7. Final Report Text — Design Patterns section (drop-in replacement)

If you still need to update the PDF report's "Design Patterns" section, here is ready-to-paste prose (matches what's in `PATTERNS.md`):

> **Design Patterns Used**
>
> Apart from the MVC architectural pattern enforced by Spring Boot, the project deliberately uses four GoF design patterns covering all three categories.
>
> **1. Factory Pattern (Creational) — `ApprovalWorkflowFactory`.** Encapsulates the creation of an `ApprovalWorkflow` behind a single `createWorkflow(...)` method. The factory receives every `ApprovalRoutingStrategy` bean from Spring and stores them in an `EnumMap<ApprovalLevel, ApprovalRoutingStrategy>`. Callers such as `OrganizerService` do not know which concrete workflow shape was built.
>
> **2. Adapter Pattern (Structural) — `ReportExporter` + `CsvReportExporter` + `PdfReportExporter`.** Adapts the internal `Budget` domain aggregate into two very different external byte-stream formats. The PDF adapter, in particular, hides the PDF 1.4 object table, font dictionary, content stream, cross-reference table and trailer behind the same `byte[] export(Budget)` method used by the CSV adapter. `FinanceService` programs only against the target interface.
>
> **3. Strategy Pattern (Behavioral) — `ApprovalRoutingStrategy` with three concrete strategies.** `AutoApprovalStrategy` creates an empty workflow, `SingleLevelApprovalStrategy` creates one step for the most capable approver, and `MultiLevelApprovalStrategy` creates a two-step chain (Coordinator → Dean). The correct strategy is selected at runtime based on the claim amount against the active `WorkflowConfig`.
>
> **4. Chain of Responsibility (Behavioral) — `ApprovalWorkflow` + `ApprovalStep`.** Each step is a handler in an ordered chain. The currently-active step is resolved by the assigned `ApprovingAuthority` via `resolve(APPROVED|REJECTED, comment)`. On approval, `ApproverService` calls `workflow.advance()` to forward the request to the next handler; on rejection, the chain terminates. A new approval tier (e.g. Registrar, Vice-Chancellor) is added by lengthening the chain inside a new strategy — no caller changes.
