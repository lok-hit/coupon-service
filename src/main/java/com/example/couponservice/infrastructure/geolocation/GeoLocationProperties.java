package com.example.couponservice.infrastructure.geolocation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Strongly-typed, validated configuration for the geo-location integration.
 * Fail-fast: if any required property is missing or invalid, the application
 * refuses to start with a clear error message.
 */
@ConfigurationProperties(prefix = "geolocation")
@Validated
@Getter
@Setter
public class GeoLocationProperties {

    /** Base URL of the geo-location provider (e.g. {@code http://ip-api.com/json}). */
    @NotBlank(message = "geolocation.provider-url must not be blank")
    private String providerUrl;

    /** TCP connect timeout; minimum 100 ms to avoid spurious failures on fast networks. */
    @NotNull(message = "geolocation.connect-timeout must not be null")
    private Duration connectTimeout;

    /** Socket read timeout; minimum 100 ms. */
    @NotNull(message = "geolocation.read-timeout must not be null")
    private Duration readTimeout;

    /** How long country codes are cached in Redis before eviction. */
    @NotNull(message = "geolocation.cache-ttl must not be null")
    private Duration cacheTtl;

    /**
     * Behaviour when the geo-location service is unavailable.
     * {@code fail-open} — allow the request (returns empty country).
     * {@code fail-closed} — reject the request with an exception.
     */
    @NotBlank(message = "geolocation.strategy must not be blank")
    @Pattern(regexp = "fail-open|fail-closed",
             message = "geolocation.strategy must be 'fail-open' or 'fail-closed'")
    private String strategy;

    /** Ensures connect timeout is at least 100 ms. */
    @AssertTrue(message = "geolocation.connect-timeout must be at least 100ms")
    public boolean isConnectTimeoutValid() {
        return connectTimeout == null || connectTimeout.toMillis() >= 100;
    }

    /** Ensures read timeout is at least 100 ms. */
    @AssertTrue(message = "geolocation.read-timeout must be at least 100ms")
    public boolean isReadTimeoutValid() {
        return readTimeout == null || readTimeout.toMillis() >= 100;
    }
}
