package com.eventbudget.controller;

import com.eventbudget.dto.BudgetResponse;
import com.eventbudget.dto.CreateBudgetRequest;
import com.eventbudget.dto.CreateEventRequest;
import com.eventbudget.dto.CreateExpenseClaimRequest;
import com.eventbudget.dto.EventResponse;
import com.eventbudget.dto.ExpenseClaimResponse;
import com.eventbudget.dto.NotificationResponse;
import com.eventbudget.security.AppUserPrincipal;
import com.eventbudget.service.OrganizerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request) {
        return organizerService.createEvent(principal.getUserId(), request);
    }

    @GetMapping("/events")
    public List<EventResponse> getMyEvents(@AuthenticationPrincipal AppUserPrincipal principal) {
        return organizerService.getMyEvents(principal.getUserId());
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateBudgetRequest request) {
        return organizerService.createBudget(principal.getUserId(), request);
    }

    @GetMapping("/budgets")
    public List<BudgetResponse> getMyBudgets(@AuthenticationPrincipal AppUserPrincipal principal) {
        return organizerService.getMyBudgets(principal.getUserId());
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseClaimResponse submitExpenseClaim(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateExpenseClaimRequest request) {
        return organizerService.submitExpenseClaim(principal.getUserId(), request);
    }

    @GetMapping("/claims")
    public List<ExpenseClaimResponse> getMyClaims(@AuthenticationPrincipal AppUserPrincipal principal) {
        return organizerService.getMyClaims(principal.getUserId());
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal AppUserPrincipal principal) {
        return organizerService.getMyNotifications(principal.getUserId());
    }
}
