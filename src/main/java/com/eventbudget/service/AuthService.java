package com.eventbudget.service;

import com.eventbudget.dto.AuthRequest;
import com.eventbudget.dto.AuthResponse;
import com.eventbudget.dto.RegisterOrganizerRequest;
import com.eventbudget.dto.UserSummaryResponse;
import com.eventbudget.exception.BusinessException;
import com.eventbudget.exception.ResourceNotFoundException;
import com.eventbudget.model.user.EventOrganizer;
import com.eventbudget.model.user.User;
import com.eventbudget.repository.EventOrganizerRepository;
import com.eventbudget.repository.UserRepository;
import com.eventbudget.security.AppUserPrincipal;
import com.eventbudget.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EventOrganizerRepository eventOrganizerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse registerOrganizer(RegisterOrganizerRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            throw new BusinessException("A user already exists with this email");
        });

        EventOrganizer organizer = new EventOrganizer();
        organizer.setName(request.name());
        organizer.setEmail(request.email());
        organizer.setDepartment(request.department());
        organizer.setPasswordHash(passwordEncoder.encode(request.password()));
        organizer = eventOrganizerRepository.save(organizer);

        return toAuthResponse(organizer);
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAuthResponse(user);
    }

    public UserSummaryResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserSummaryResponse.from(user);
    }

    private AuthResponse toAuthResponse(User user) {
        AppUserPrincipal principal = AppUserPrincipal.from(user);
        return new AuthResponse(
                jwtService.generateToken(principal),
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name());
    }
}
