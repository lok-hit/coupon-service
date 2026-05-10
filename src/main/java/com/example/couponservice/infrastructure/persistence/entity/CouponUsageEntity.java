package com.example.couponservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code coupon_usage} table.
 *
 * <p>Thread-safety: instances are not shared across threads. Each HTTP request gets
 * its own EntityManager via HikariCP connection-per-request.
 */
@Entity
@Table(name = "coupon_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsageEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String couponCode;

    @Column(nullable = false, length = 255)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime usedAt;

    @Column(nullable = false, length = 45)
    private String sourceIp;

    @Column(nullable = false, unique = true, length = 36)
    private String idempotencyKey;
}