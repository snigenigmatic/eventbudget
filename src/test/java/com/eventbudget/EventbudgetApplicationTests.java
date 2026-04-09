package com.eventbudget;

import com.eventbudget.model.domain.ExpenseCategory;
import com.eventbudget.repository.ExpenseCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventbudgetApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Test
    void organizerBudgetAndClaimApprovalFlowWorks() throws Exception {
        String organizerToken = login("organizer@eventbudget.local", "password123");
        String approverToken = login("coordinator@eventbudget.local", "password123");

        MvcResult eventResult = mockMvc.perform(post("/api/organizer/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Tech Symposium",
                                "description", "Annual symposium",
                                "eventDate", LocalDate.now().toString(),
                                "venue", "Seminar Hall"))))
                .andExpect(status().isCreated())
                .andReturn();

        long eventId = readTree(eventResult).get("eventId").asLong();
        long travelExpenseCategoryId = findExpenseCategoryId("Travel");
        long marketingExpenseCategoryId = findExpenseCategoryId("Marketing");

        MvcResult budgetResult = mockMvc.perform(post("/api/organizer/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", eventId,
                                "totalAmount", 5000,
                                "categories", new Object[] {
                                        Map.of("expenseCategoryId", travelExpenseCategoryId, "allocatedAmount", 3000),
                                        Map.of("expenseCategoryId", marketingExpenseCategoryId, "allocatedAmount", 2000)
                                }))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn();

        JsonNode budgetJson = readTree(budgetResult);
        long budgetId = budgetJson.get("budgetId").asLong();
        long travelBudgetCategoryId = findBudgetCategoryId(budgetJson, "Travel");

        mockMvc.perform(post("/api/approver/budgets/{budgetId}/decision", budgetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approved", true,
                                "comment", "Budget is within allocation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        MvcResult claimResult = mockMvc.perform(post("/api/organizer/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "budgetId", budgetId,
                                "budgetCategoryId", travelBudgetCategoryId,
                                "vendor", "City Travels",
                                "amount", 1000,
                                "description", "Speaker airport transfer",
                                "expenseDate", LocalDate.now().toString(),
                                "documents", new Object[] {
                                        Map.of(
                                                "fileName", "invoice.pdf",
                                                "fileType", "application/pdf",
                                                "storageUrl", "https://docs.local/invoice.pdf")
                                }))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.approvalLevel").value("SINGLE_LEVEL"))
                .andReturn();

        long claimId = readTree(claimResult).get("claimId").asLong();

        mockMvc.perform(get("/api/approver/claims/pending")
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(claimId));

        mockMvc.perform(post("/api/approver/claims/{claimId}/decision", claimId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "comment", "Approved after reviewing invoice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/organizer/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(claimId))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void autoApproveClaimWorks() throws Exception {
        // Marketing category has no mandatory approval; ₹500 < autoApproveLimit (₹2000) → AUTO_APPROVED
        String organizerToken = login("organizer@eventbudget.local", "password123");
        String approverToken  = login("coordinator@eventbudget.local", "password123");

        long eventId = readTree(mockMvc.perform(post("/api/organizer/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Marketing Workshop",
                                "description", "Annual marketing workshop",
                                "eventDate", LocalDate.now().toString(),
                                "venue", "Conference Room A"))))
                .andExpect(status().isCreated()).andReturn()).get("eventId").asLong();

        long marketingCatId = findExpenseCategoryId("Marketing");

        MvcResult budgetResult = mockMvc.perform(post("/api/organizer/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", eventId,
                                "totalAmount", 5000,
                                "categories", new Object[]{
                                        Map.of("expenseCategoryId", marketingCatId, "allocatedAmount", 5000)
                                }))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn();

        long budgetId           = readTree(budgetResult).get("budgetId").asLong();
        long marketingBudgetCatId = findBudgetCategoryId(readTree(budgetResult), "Marketing");

        mockMvc.perform(post("/api/approver/budgets/{id}/decision", budgetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true, "comment", "Approved"))))
                .andExpect(status().isOk());

        // Claim ₹500 — should auto-approve without any approver action
        mockMvc.perform(post("/api/organizer/claims")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "budgetId", budgetId,
                                "budgetCategoryId", marketingBudgetCatId,
                                "vendor", "Print Shop",
                                "amount", 500,
                                "description", "Banner printing",
                                "expenseDate", LocalDate.now().toString(),
                                "documents", new Object[]{}))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AUTO_APPROVED"))
                .andExpect(jsonPath("$.approvalLevel").value("AUTO_APPROVED"));
    }

    @Test
    void budgetRejectionFlowWorks() throws Exception {
        String organizerToken = login("organizer@eventbudget.local", "password123");
        String approverToken  = login("coordinator@eventbudget.local", "password123");

        long eventId = readTree(mockMvc.perform(post("/api/organizer/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Cultural Fest",
                                "description", "Annual cultural festival",
                                "eventDate", LocalDate.now().plusMonths(2).toString(),
                                "venue", "Main Auditorium"))))
                .andExpect(status().isCreated()).andReturn()).get("eventId").asLong();

        long venueCatId = findExpenseCategoryId("Venue");

        long budgetId = readTree(mockMvc.perform(post("/api/organizer/budgets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "eventId", eventId,
                                "totalAmount", 8000,
                                "categories", new Object[]{
                                        Map.of("expenseCategoryId", venueCatId, "allocatedAmount", 8000)
                                }))))
                .andExpect(status().isCreated()).andReturn()).get("budgetId").asLong();

        mockMvc.perform(post("/api/approver/budgets/{id}/decision", budgetId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approved", false,
                                "comment", "Budget exceeds event size requirements"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Rejected budget should not appear in pending list
        mockMvc.perform(get("/api/approver/budgets/pending")
                        .header(HttpHeaders.AUTHORIZATION, bearer(approverToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.budgetId == " + budgetId + ")]").doesNotExist());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        String token = readTree(result).get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long findExpenseCategoryId(String name) {
        ExpenseCategory category = expenseCategoryRepository.findByNameIgnoreCase(name)
                .orElseThrow();
        return category.getCategoryId();
    }

    private long findBudgetCategoryId(JsonNode budgetJson, String expenseCategoryName) {
        for (JsonNode category : budgetJson.get("categories")) {
            if (expenseCategoryName.equals(category.get("expenseCategoryName").asText())) {
                return category.get("categoryId").asLong();
            }
        }
        throw new IllegalArgumentException("Budget category not found for " + expenseCategoryName);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
