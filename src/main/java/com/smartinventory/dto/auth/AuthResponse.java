package com.smartinventory.dto.auth;

import lombok.Builder;

import java.util.Set;

/** Response returned after a successful login/registration. */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        Long userId,
        String username,
        String fullName,
        Set<String> roles
) {}
