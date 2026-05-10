package com.example.couponservice.infrastructure.persistence.entity;

import com.example.couponservice.domain.model.Coupon;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code coupon} table.
 *
 * <p>Thread-safety: instances are not shared across threads. Each HTTP request gets
 * its own EntityManager (and thus its own entity instances) via HikariCP connection-per-request.
 * The {@link Version} field provides optimistic locking for metadata updates; it is NOT used
 * for {@code currentUses} — that column is protected by the CAS UPDATE in
 * {@link com.example.couponservice.infrastructure.persistence.JpaCouponRepository#incrementUsage}.
 */
@Entity
@Table(name = "coupon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private int maxUses;

    @Column(nullable = false)
    private int currentUses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Coupon.Status status;

    private LocalDateTime validUntil;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Optimistic-locking version for metadata fields (code, country, maxUses, status,
     * validUntil). Not involved in the CAS increment of {@code currentUses}.
     */
    @Version
    private Long version;
}