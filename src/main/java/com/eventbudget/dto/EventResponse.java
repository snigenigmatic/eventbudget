package com.eventbudget.dto;

import com.eventbudget.model.domain.Event;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventResponse(
        Long eventId,
        String name,
        String description,
        LocalDate eventDate,
        String venue,
        UserSummaryResponse organizer,
        Long budgetId,
        LocalDateTime createdAt
) {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getEventId(),
                event.getName(),
                event.getDescription(),
                event.getEventDate(),
                event.getVenue(),
                UserSummaryResponse.from(event.getOrganizer()),
                event.getBudget() != null ? event.getBudget().getBudgetId() : null,
                event.getCreatedAt());
    }
}
