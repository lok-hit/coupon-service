package com.example.couponservice.infrastructure.cache;

import com.example.couponservice.domain.model.CouponUsage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RedisIdempotencyStore}. No Spring context — uses Mockito mocks.
 */
@SuppressWarnings("unchecked")
class RedisIdempotencyStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private ObjectMapper objectMapper;
    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        store = new RedisIdempotencyStore(redisTemplate, objectMapper);
    }

    private CouponUsage sampleUsage() {
        return new CouponUsage(
                UUID.randomUUID(),
                "SUMMER10",
                "user-123",
                LocalDateTime.of(2024, 6, 1, 12, 0, 0),
                "192.168.1.0",
                "idem-key-001");
    }

    @Test
    void find_keyExists_returnsDeserializedCouponUsage() throws Exception {
        CouponUsage expected = sampleUsage();
        String json = objectMapper.writeValueAsString(expected);
        when(valueOps.get("idempotency:idem-key-001")).thenReturn(json);

        Optional<CouponUsage> result = store.find("idem-key-001");

        assertThat(result).isPresent();
        assertThat(result.get().idempotencyKey()).isEqualTo("idem-key-001");
        assertThat(result.get().couponCode()).isEqualTo("SUMMER10");
        assertThat(result.get().userId()).isEqualTo("user-123");
    }

    @Test
    void find_keyMissing_returnsEmpty() {
        when(valueOps.get("idempotency:nonexistent")).thenReturn(null);

        Optional<CouponUsage> result = store.find("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void find_redisException_returnsEmptyAndDoesNotPropagate() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        Optional<CouponUsage> result = store.find("idem-key-001");

        assertThat(result).isEmpty();
    }

    @Test
    void store_serializesAndStoresWith24hTtl() throws Exception {
        CouponUsage usage = sampleUsage();

        store.store("idem-key-001", usage);

        verify(valueOps).set(
                eq("idempotency:idem-key-001"),
                argThat(json -> json.contains("SUMMER10") && json.contains("user-123")),
                eq(Duration.ofHours(24)));
    }

    @Test
    void store_redisException_doesNotPropagate() {
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        store.store("idem-key-001", sampleUsage());
        // must not throw
    }
}