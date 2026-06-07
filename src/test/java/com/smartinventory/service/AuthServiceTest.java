package com.smartinventory.service;

import com.smartinventory.dto.auth.AuthResponse;
import com.smartinventory.dto.auth.RegisterRequest;
import com.smartinventory.entity.Role;
import com.smartinventory.entity.User;
import com.smartinventory.enums.RoleName;
import com.smartinventory.exception.DuplicateResourceException;
import com.smartinventory.repository.RoleRepository;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — registration & login")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("registering a new user returns a JWT and defaults role to STAFF")
    void register_success_defaultsToStaff() {
        var req = new RegisterRequest("alice", "Alice A", "alice@x.com", "secret123", null);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@x.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.STAFF))
                .thenReturn(Optional.of(Role.builder().id(3L).name(RoleName.STAFF).build()));
        when(passwordEncoder.encode("secret123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0); u.setId(99L); return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.register(req);

        assertThat(resp.accessToken()).isEqualTo("access-token");
        assertThat(resp.username()).isEqualTo("alice");
        assertThat(resp.roles()).containsExactly("STAFF");
        verify(passwordEncoder).encode("secret123");   // never stores plaintext
    }

    @Test
    @DisplayName("registering a duplicate username is rejected")
    void register_duplicateUsername_throws() {
        var req = new RegisterRequest("bob", "Bob B", "bob@x.com", "secret123", Set.of(RoleName.MANAGER));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
