package com.eventbudget.dto;

import com.eventbudget.model.user.User;

public record UserSummaryResponse(
        Long userId,
        String name,
        String email,
        String role
) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name());
    }
}
