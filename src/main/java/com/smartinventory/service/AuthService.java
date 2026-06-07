package com.smartinventory.service;

import com.smartinventory.dto.auth.AuthResponse;
import com.smartinventory.dto.auth.LoginRequest;
import com.smartinventory.dto.auth.RegisterRequest;
import com.smartinventory.entity.Role;
import com.smartinventory.entity.User;
import com.smartinventory.enums.RoleName;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.repository.RoleRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.CustomUserDetails;
import com.smartinventory.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Handles registration and authentication, issuing JWTs on success. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new DuplicateResourceException("Username already taken: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Email already registered: " + req.email());
        }

        Set<RoleName> requested = (req.roles() == null || req.roles().isEmpty())
                ? Set.of(RoleName.STAFF)
                : req.roles();

        Set<Role> roles = requested.stream()
                .map(rn -> roleRepository.findByName(rn)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", rn)))
                .collect(Collectors.toCollection(HashSet::new));

        User user = User.builder()
                .username(req.username())
                .fullName(req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .enabled(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);
        log.info("Registered new user '{}' with roles {}", user.getUsername(), requested);

        return buildAuthResponse(new CustomUserDetails(user), user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.usernameOrEmail(), req.password()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        log.info("User '{}' logged in", principal.getUsername());
        return buildAuthResponse(principal, user);
    }

    private AuthResponse buildAuthResponse(CustomUserDetails principal, User user) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roles(roleNames)
                .build();
    }
}
