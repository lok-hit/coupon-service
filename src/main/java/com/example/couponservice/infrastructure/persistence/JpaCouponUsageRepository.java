package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.infrastructure.persistence.entity.CouponUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CouponUsageEntity}.
 *
 * <p>Spring Data proxy is thread-safe; each method uses a connection from HikariCP.
 */
public interface JpaCouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {

    /** Returns {@code true} if the user has already used the given coupon. */
    boolean existsByUserIdAndCouponCode(String userId, String couponCode);

    /** Finds a usage record by its idempotency key. */
    Optional<CouponUsageEntity> findByIdempotencyKey(String idempotencyKey);
}