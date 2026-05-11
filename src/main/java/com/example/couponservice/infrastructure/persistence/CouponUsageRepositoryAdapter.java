package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.domain.port.out.CouponUsageRepository;
import com.example.couponservice.infrastructure.cache.RedisIdempotencyStore;
import com.example.couponservice.infrastructure.persistence.mapper.CouponUsageMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter that bridges the domain {@link CouponUsageRepository} output port to Spring Data JPA,
 * with a Redis read-through cache for idempotency lookups.
 *
 * <p>Thread-safety: this class is stateless. HikariCP provides connection-per-request isolation;
 * {@link RedisIdempotencyStore} and {@link org.springframework.data.jpa.repository.JpaRepository}
 * proxies are both thread-safe. No synchronization is needed.
 */
@Repository
public class CouponUsageRepositoryAdapter implements CouponUsageRepository {

    private final JpaCouponUsageRepository jpaRepository;
    private final CouponUsageMapper mapper;
    private final RedisIdempotencyStore redisIdempotencyStore;

    public CouponUsageRepositoryAdapter(
            JpaCouponUsageRepository jpaRepository,
            CouponUsageMapper mapper,
            RedisIdempotencyStore redisIdempotencyStore) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.redisIdempotencyStore = redisIdempotencyStore;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCouponCode(String userId, String couponCode) {
        return jpaRepository.existsByUserIdAndCouponCode(userId, couponCode);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks Redis first for sub-millisecond lookups on hot keys. Falls back to
     * the database if the key is absent from Redis (cache miss or TTL expiry).
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CouponUsage> findByIdempotencyKey(String idempotencyKey) {
        Optional<CouponUsage> cached = redisIdempotencyStore.find(idempotencyKey);
        if (cached.isPresent()) {
            return cached;
        }
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toDomain);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Persists to the database and writes through to Redis so that the next
     * idempotency check is served from the cache.
     */
    @Override
    @Transactional
    public CouponUsage save(CouponUsage usage) {
        CouponUsage saved = mapper.toDomain(jpaRepository.save(mapper.toEntity(usage)));
        redisIdempotencyStore.store(usage.idempotencyKey(), saved);
        return saved;
    }
}