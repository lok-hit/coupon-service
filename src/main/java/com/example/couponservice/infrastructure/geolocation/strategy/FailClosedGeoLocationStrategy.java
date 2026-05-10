package com.example.couponservice.infrastructure.geolocation.strategy;

import com.example.couponservice.domain.exception.GeoLocationUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Fail-closed strategy: when geo-location is unavailable, rejects the request by throwing
 * {@link GeoLocationUnavailableException}. Suitable for production environments where
 * geo-based restrictions must be enforced strictly.
 *
 * <p>Active when {@code geolocation.strategy=fail-closed} (also the default when the
 * property is absent).
 */
@Component
@ConditionalOnProperty(name = "geolocation.strategy", havingValue = "fail-closed", matchIfMissing = true)
public class FailClosedGeoLocationStrategy implements GeoLocationStrategy {

    /**
     * {@inheritDoc}
     *
     * @throws GeoLocationUnavailableException always, to reject the request
     */
    @Override
    public Optional<String> handleUnavailable(String ip) {
        throw new GeoLocationUnavailableException(ip);
    }
}