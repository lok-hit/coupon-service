package com.example.couponservice.infrastructure.geolocation.strategy;

import java.util.Optional;

/**
 * Strategy that decides what to do when the geo-location service is unavailable
 * (network error, timeout, circuit-breaker open, or empty response).
 */
public interface GeoLocationStrategy {

    /**
     * Handles the case where geo-location is unavailable for the given IP.
     *
     * @param ip the IP whose country could not be determined
     * @return an empty {@link Optional} (fail-open) or throws
     *         {@link com.example.couponservice.domain.exception.GeoLocationUnavailableException}
     *         (fail-closed)
     */
    Optional<String> handleUnavailable(String ip);
}