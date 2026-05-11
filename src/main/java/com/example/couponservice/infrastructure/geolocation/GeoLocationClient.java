package com.example.couponservice.infrastructure.geolocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * HTTP client for the geo-location provider. Uses Spring {@link RestClient} (not RestTemplate,
 * not WebClient) for synchronous, blocking HTTP calls.
 *
 * <p>Thread-safety: {@link RestClient} is stateless and fully thread-safe. All calls are
 * pure function applications with no shared mutable state in this class.
 */
@Component
public class GeoLocationClient {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationClient.class);

    private final RestClient restClient;

    /**
     * Constructs the client with a pre-configured {@link RestClient} (base URL + timeouts
     * are set in {@link GeoLocationConfiguration}).
     *
     * @param geoLocationRestClient a RestClient already configured for the provider
     */
    public GeoLocationClient(RestClient geoLocationRestClient) {
        this.restClient = geoLocationRestClient;
    }

    /**
     * Fetches the country code for the given IP address from the remote provider.
     *
     * <p>On any exception (network error, timeout, HTTP error, malformed JSON) the
     * error is logged at WARN level and {@link Optional#empty()} is returned so the
     * caller can apply its availability strategy.
     *
     * @param ip the IP address to look up
     * @return an {@link Optional} containing the {@link GeoLocationResponse}, or empty on failure
     */
    public Optional<GeoLocationResponse> fetchCountry(String ip) {
        try {
            GeoLocationResponse response = restClient.get()
                    .uri("/{ip}", ip)
                    .retrieve()
                    .body(GeoLocationResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.warn("Geo-location lookup failed for IP {}: {}", ip, e.getMessage());
            return Optional.empty();
        }
    }
}