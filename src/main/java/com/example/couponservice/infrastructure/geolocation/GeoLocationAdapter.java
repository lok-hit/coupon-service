package com.example.couponservice.infrastructure.geolocation;

import com.example.couponservice.domain.port.out.GeoLocationPort;
import com.example.couponservice.infrastructure.geolocation.strategy.GeoLocationStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Adapter implementing {@link GeoLocationPort} with Redis caching and Resilience4j fault tolerance.
 *
 * <p>Thread-safety: this class is stateless. All dependencies ({@link GeoLocationClient},
 * {@link StringRedisTemplate}, {@link CircuitBreaker}, {@link Retry}) are thread-safe.
 * The compiled {@link Pattern} is immutable. No synchronization is needed.
 *
 * <p>Execution order per call:
 * <ol>
 *   <li>Private IP → return {@link Optional#empty()} immediately (no network call).</li>
 *   <li>Redis cache hit → return cached country code.</li>
 *   <li>Cache miss → {@code Retry(CircuitBreaker(GeoLocationClient.fetchCountry(ip)))}.</li>
 *   <li>Success → store in Redis with TTL, return country code.</li>
 *   <li>Empty response or any failure → delegate to {@link GeoLocationStrategy}.</li>
 * </ol>
 */
@Component
public class GeoLocationAdapter implements GeoLocationPort {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationAdapter.class);

    private static final String CIRCUIT_BREAKER_NAME = "geoLocation";
    private static final String REDIS_KEY_PREFIX = "geo:";

    private static final Pattern PRIVATE_IP = Pattern.compile(
            "^(127\\..*" +
            "|10\\..*" +
            "|172\\.(1[6-9]|2[0-9]|3[01])\\..*" +
            "|192\\.168\\..*)$");

    private final GeoLocationClient client;
    private final GeoLocationStrategy strategy;
    private final StringRedisTemplate redisTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final GeoLocationProperties properties;

    public GeoLocationAdapter(
            GeoLocationClient client,
            GeoLocationStrategy strategy,
            StringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            GeoLocationProperties properties) {
        this.client = client;
        this.strategy = strategy;
        this.redisTemplate = redisTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.retry = retryRegistry.retry(CIRCUIT_BREAKER_NAME);
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Private IPs (loopback, RFC-1918) always return empty without an external call.
     * Decorator order: Retry (outermost) → CircuitBreaker → GeoLocationClient.fetchCountry().
     */
    @Override
    public Optional<String> getCountryCode(String ip) {
        if (isPrivateIp(ip)) {
            return Optional.empty();
        }

        String cached = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + ip);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Decorator order: Retry → CircuitBreaker → client call
        Supplier<Optional<GeoLocationResponse>> decorated =
                Retry.decorateSupplier(retry,
                        CircuitBreaker.decorateSupplier(circuitBreaker,
                                () -> client.fetchCountry(ip)));
        try {
            Optional<GeoLocationResponse> response = decorated.get();

            if (response.isPresent() && response.get().success()) {
                String countryCode = response.get().countryCode();
                redisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + ip, countryCode, properties.getCacheTtl());
                return Optional.of(countryCode);
            }

            return strategy.handleUnavailable(ip);

        } catch (Exception e) {
            log.warn("Geo-location resilience layer failed for IP {}: {}", ip, e.getMessage());
            return strategy.handleUnavailable(ip);
        }
    }

    private boolean isPrivateIp(String ip) {
        return ip != null && PRIVATE_IP.matcher(ip).matches();
    }
}