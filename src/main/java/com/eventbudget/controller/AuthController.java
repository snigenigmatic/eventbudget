package com.eventbudget.controller;

import com.eventbudget.dto.AuthRequest;
import com.eventbudget.dto.AuthResponse;
import com.eventbudget.dto.RegisterOrganizerRequest;
import com.eventbudget.dto.UserSummaryResponse;
import com.eventbudget.security.AppUserPrincipal;
import com.eventbudget.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterOrganizerRequest request) {
        return authService.registerOrganizer(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.getCurrentUser(principal.getUserId());
    }
}
