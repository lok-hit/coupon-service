package com.example.couponservice.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload representing a coupon.
 */
public record CouponResponse(
        UUID id,
        String code,
        String country,
        int maxUses,
        int currentUses,
        String status,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {}
