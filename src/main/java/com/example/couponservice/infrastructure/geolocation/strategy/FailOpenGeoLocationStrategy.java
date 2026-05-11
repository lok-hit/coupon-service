package com.example.couponservice.infrastructure.geolocation.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fail-open strategy: when geo-location is unavailable, allows the request to proceed
 * by returning {@link Optional#empty()}. Suitable for development and staging environments
 * where geo-based restrictions should not block traffic.
 *
 * <p>Active when {@code geolocation.strategy=fail-open}.
 */
@Component
@ConditionalOnProperty(name = "geolocation.strategy", havingValue = "fail-open")
public class FailOpenGeoLocationStrategy implements GeoLocationStrategy {

    private static final Logger log = LoggerFactory.getLogger(FailOpenGeoLocationStrategy.class);

    /**
     * {@inheritDoc}
     *
     * <p>Logs a WARN and returns empty, allowing the caller to proceed without a country code.
     */
    @Override
    public Optional<String> handleUnavailable(String ip) {
        log.warn("Geo-location unavailable for IP {} — failing open (request allowed)", ip);
        return Optional.empty();
    }
}