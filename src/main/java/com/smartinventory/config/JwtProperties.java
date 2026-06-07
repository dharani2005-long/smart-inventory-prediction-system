package com.smartinventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.jwt.*} configuration.
 *
 * @param secret              Base64-encoded HMAC signing key (256-bit minimum)
 * @param expirationMs        access-token lifetime in milliseconds
 * @param refreshExpirationMs refresh-token lifetime in milliseconds
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs) {
}
