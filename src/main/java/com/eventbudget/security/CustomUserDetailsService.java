package com.eventbudget.security;

import com.eventbudget.exception.ResourceNotFoundException;
import com.eventbudget.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + username));
    }
}
