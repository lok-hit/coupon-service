package com.example.couponservice.infrastructure.geolocation;

import com.example.couponservice.infrastructure.geolocation.strategy.GeoLocationStrategy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GeoLocationAdapter}. No Spring context — uses in-memory
 * Resilience4j registries and Mockito mocks.
 */
@SuppressWarnings("unchecked")
class GeoLocationAdapterTest {

    private GeoLocationClient client;
    private GeoLocationStrategy strategy;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private GeoLocationProperties properties;

    private CircuitBreakerRegistry cbRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        client = mock(GeoLocationClient.class);
        strategy = mock(GeoLocationStrategy.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        properties = new GeoLocationProperties();
        properties.setProviderUrl("http://ip-api.com/json");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(5));
        properties.setCacheTtl(Duration.ofHours(1));
        properties.setStrategy("fail-open");

        cbRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.of(
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(0))
                        .build());
    }

    private GeoLocationAdapter adapter() {
        return new GeoLocationAdapter(client, strategy, redisTemplate, cbRegistry, retryRegistry, properties);
    }

    @Test
    void getCountryCode_privateIp_returnsEmptyWithoutAnyCall() {
        GeoLocationAdapter adapter = adapter();

        Optional<String> result = adapter.getCountryCode("127.0.0.1");

        assertThat(result).isEmpty();
        verifyNoInteractions(redisTemplate, client, strategy);
    }

    @Test
    void getCountryCode_privateIp10Block_returnsEmpty() {
        Optional<String> result = adapter().getCountryCode("10.0.0.1");
        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void getCountryCode_cacheHit_returnsCountryFromRedis() {
        when(valueOps.get("geo:1.2.3.4")).thenReturn("PL");

        Optional<String> result = adapter().getCountryCode("1.2.3.4");

        assertThat(result).contains("PL");
        verifyNoInteractions(client);
    }

    @Test
    void getCountryCode_cacheMissAndSuccessResponse_returnsCountryAndStoresInRedis() {
        when(valueOps.get("geo:1.2.3.4")).thenReturn(null);
        when(client.fetchCountry("1.2.3.4"))
                .thenReturn(Optional.of(new GeoLocationResponse("success", "DE")));

        Optional<String> result = adapter().getCountryCode("1.2.3.4");

        assertThat(result).contains("DE");
        verify(valueOps).set(eq("geo:1.2.3.4"), eq("DE"), eq(Duration.ofHours(1)));
        verifyNoInteractions(strategy);
    }

    @Test
    void getCountryCode_cacheMissAndEmptyResponse_delegatesToStrategy() {
        when(valueOps.get("geo:1.2.3.4")).thenReturn(null);
        when(client.fetchCountry("1.2.3.4")).thenReturn(Optional.empty());
        when(strategy.handleUnavailable("1.2.3.4")).thenReturn(Optional.empty());

        adapter().getCountryCode("1.2.3.4");

        verify(strategy).handleUnavailable("1.2.3.4");
    }

    @Test
    void getCountryCode_cacheMissAndClientThrows_delegatesToStrategyAfterRetriesExhausted() {
        when(valueOps.get("geo:1.2.3.4")).thenReturn(null);
        when(client.fetchCountry("1.2.3.4"))
                .thenThrow(new RuntimeException("connection refused"));
        when(strategy.handleUnavailable("1.2.3.4")).thenReturn(Optional.empty());

        adapter().getCountryCode("1.2.3.4");

        // maxAttempts=3: client called 3 times total, then strategy invoked once
        verify(client, times(3)).fetchCountry("1.2.3.4");
        verify(strategy).handleUnavailable("1.2.3.4");
    }

    @Test
    void getCountryCode_circuitBreakerOpen_delegatesToStrategyImmediately() {
        CircuitBreaker cb = cbRegistry.circuitBreaker("geoLocation");
        cb.transitionToOpenState();

        when(valueOps.get("geo:1.2.3.4")).thenReturn(null);
        when(strategy.handleUnavailable("1.2.3.4")).thenReturn(Optional.empty());

        adapter().getCountryCode("1.2.3.4");

        // Circuit is open — client must never be called
        verifyNoInteractions(client);
        verify(strategy).handleUnavailable("1.2.3.4");
    }
}