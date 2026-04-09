package com.eventbudget.config;

import com.eventbudget.model.approval.WorkflowConfig;
import com.eventbudget.model.domain.CategoryRule;
import com.eventbudget.model.domain.ExpenseCategory;
import com.eventbudget.model.user.ApprovingAuthority;
import com.eventbudget.model.user.EventOrganizer;
import com.eventbudget.model.user.FinanceAdmin;
import com.eventbudget.repository.ApprovingAuthorityRepository;
import com.eventbudget.repository.EventOrganizerRepository;
import com.eventbudget.repository.ExpenseCategoryRepository;
import com.eventbudget.repository.FinanceAdminRepository;
import com.eventbudget.repository.WorkflowConfigRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final EventOrganizerRepository eventOrganizerRepository;
    private final ApprovingAuthorityRepository approvingAuthorityRepository;
    private final FinanceAdminRepository financeAdminRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final WorkflowConfigRepository workflowConfigRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedUsers();
            seedExpenseCategories();
            seedWorkflowConfig();
        };
    }

    private void seedUsers() {
        if (eventOrganizerRepository.count() == 0) {
            EventOrganizer organizer = new EventOrganizer();
            organizer.setName("Campus Organizer");
            organizer.setEmail("organizer@eventbudget.local");
            organizer.setDepartment("Student Affairs");
            organizer.setPasswordHash(passwordEncoder.encode("password123"));
            eventOrganizerRepository.save(organizer);
        }

        if (approvingAuthorityRepository.count() == 0) {
            ApprovingAuthority coordinator = new ApprovingAuthority();
            coordinator.setName("Faculty Coordinator");
            coordinator.setEmail("coordinator@eventbudget.local");
            coordinator.setDesignation("Faculty Coordinator");
            coordinator.setAuthorizationLimit(new BigDecimal("1000000"));
            coordinator.setPasswordHash(passwordEncoder.encode("password123"));
            approvingAuthorityRepository.save(coordinator);

            ApprovingAuthority dean = new ApprovingAuthority();
            dean.setName("Dean Approval");
            dean.setEmail("dean@eventbudget.local");
            dean.setDesignation("Dean");
            dean.setAuthorizationLimit(new BigDecimal("100000"));
            dean.setPasswordHash(passwordEncoder.encode("password123"));
            approvingAuthorityRepository.save(dean);
        }

        if (financeAdminRepository.count() == 0) {
            FinanceAdmin financeAdmin = new FinanceAdmin();
            financeAdmin.setName("Finance Office");
            financeAdmin.setEmail("finance@eventbudget.local");
            financeAdmin.setPasswordHash(passwordEncoder.encode("password123"));
            financeAdminRepository.save(financeAdmin);
        }
    }

    private void seedExpenseCategories() {
        if (expenseCategoryRepository.count() > 0) {
            return;
        }

        expenseCategoryRepository.save(createCategory(
                "Venue",
                "Venue booking and facility expenses",
                new BigDecimal("20000"),
                false,
                true,
                "Venue invoices are mandatory"));

        expenseCategoryRepository.save(createCategory(
                "Food",
                "Catering and refreshments",
                new BigDecimal("15000"),
                false,
                true,
                "Food claims require bills"));

        expenseCategoryRepository.save(createCategory(
                "Marketing",
                "Posters, social promotion and publicity",
                new BigDecimal("8000"),
                false,
                false,
                "Marketing must stay within the allocated amount"));

        expenseCategoryRepository.save(createCategory(
                "Travel",
                "Speaker and logistics travel",
                new BigDecimal("25000"),
                true,
                true,
                "Travel reimbursements always require documents"));
    }

    private ExpenseCategory createCategory(
            String name,
            String description,
            BigDecimal maxAmount,
            boolean requiresMandatoryApproval,
            boolean mandatoryDocumentRequired,
            String ruleDescription) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        category.setDescription(description);

        CategoryRule rule = new CategoryRule();
        rule.setCategory(category);
        rule.setMaxAmount(maxAmount);
        rule.setRequiresMandatoryApproval(requiresMandatoryApproval);
        rule.setMandatoryDocumentRequired(mandatoryDocumentRequired);
        rule.setDescription(ruleDescription);
        category.getRules().add(rule);

        return category;
    }

    private void seedWorkflowConfig() {
        if (workflowConfigRepository.count() > 0) {
            return;
        }

        WorkflowConfig config = new WorkflowConfig();
        config.setName("Default Approval Policy");
        config.setAutoApproveLimit(new BigDecimal("2000"));
        config.setSingleLevelLimit(new BigDecimal("10000"));
        config.setMultiLevelThreshold(new BigDecimal("10000"));
        workflowConfigRepository.save(config);
    }
}
