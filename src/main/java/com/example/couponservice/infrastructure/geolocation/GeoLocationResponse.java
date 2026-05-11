package com.example.couponservice.infrastructure.geolocation;

/**
 * Deserialized response from the geo-location HTTP provider.
 *
 * @param status      provider status string; {@code "success"} on success
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g. {@code "PL"})
 */
public record GeoLocationResponse(String status, String countryCode) {

    /** Returns {@code true} when the provider reported a successful lookup. */
    public boolean success() {
        return "success".equals(status);
    }
}