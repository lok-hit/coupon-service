package com.example.couponservice.infrastructure.cache;

import com.example.couponservice.domain.model.CouponUsage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed store for idempotency keys, preventing duplicate coupon redemptions
 * caused by client retries.
 *
 * <p>Thread-safety: {@link StringRedisTemplate} is thread-safe (uses a connection pool).
 * {@link ObjectMapper} is thread-safe after construction. This class has no mutable state.
 * No synchronization is needed.
 */
@Component
public class RedisIdempotencyStore {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyStore.class);

    // Business rule: idempotency window matches typical retry windows; not configurable.
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves a previously stored {@link CouponUsage} by idempotency key.
     *
     * <p>On any Redis or deserialization error, logs WARN and returns empty so the
     * caller falls through to the database.
     *
     * @param idempotencyKey the client-supplied idempotency key
     * @return the stored usage if found, or {@link Optional#empty()}
     */
    public Optional<CouponUsage> find(String idempotencyKey) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CouponUsage.class));
        } catch (Exception e) {
            log.warn("Failed to read idempotency key {} from Redis: {}", idempotencyKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Stores a {@link CouponUsage} under the given idempotency key with a 24-hour TTL.
     *
     * <p>On any Redis or serialization error, logs WARN and swallows the exception so
     * that a Redis outage never blocks a successful redemption.
     *
     * @param idempotencyKey the client-supplied idempotency key
     * @param usage          the usage record to cache
     */
    public void store(String idempotencyKey, CouponUsage usage) {
        try {
            String json = objectMapper.writeValueAsString(usage);
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, IDEMPOTENCY_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize CouponUsage for idempotency key {}: {}", idempotencyKey, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to store idempotency key {} in Redis: {}", idempotencyKey, e.getMessage());
        }
    }
}