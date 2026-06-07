package com.smartinventory.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Login credentials — accepts username or email in {@code usernameOrEmail}. */
public record LoginRequest(

        @NotBlank(message = "Username or email is required")
        String usernameOrEmail,

        @NotBlank(message = "Password is required")
        String password
) {}
