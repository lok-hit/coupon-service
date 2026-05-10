package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.infrastructure.persistence.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link CouponEntity}.
 *
 * <p>Spring Data proxy is thread-safe; each method uses a connection from HikariCP.
 */
public interface JpaCouponRepository extends JpaRepository<CouponEntity, UUID> {

    /** Finds a coupon by its unique code. */
    Optional<CouponEntity> findByCode(String code);

    /**
     * CAS-style atomic increment of {@code currentUses}. The WHERE clause prevents
     * exceeding {@code maxUses} without a distributed lock.
     *
     * @param code the coupon code to increment
     * @return 1 if the row was updated, 0 if {@code currentUses >= maxUses}
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CouponEntity c SET c.currentUses = c.currentUses + 1 " +
           "WHERE c.code = :code AND c.currentUses < c.maxUses")
    int incrementUsage(@Param("code") String code);
}