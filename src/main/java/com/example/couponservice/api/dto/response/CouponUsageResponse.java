package com.example.couponservice.api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload representing a coupon usage record.
 */
public record CouponUsageResponse(
        UUID id,
        String couponCode,
        String userId,
        LocalDateTime usedAt
) {}
