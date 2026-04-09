package com.eventbudget.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateEventRequest(
        @NotBlank String name,
        String description,
        @NotNull @FutureOrPresent LocalDate eventDate,
        @NotBlank String venue
) {
}
