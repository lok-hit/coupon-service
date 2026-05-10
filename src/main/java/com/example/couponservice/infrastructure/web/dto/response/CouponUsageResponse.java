package com.example.couponservice.infrastructure.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CouponUsageResponse(
        UUID id,
        String couponCode,
        String userId,
        LocalDateTime usedAt,
        String sourceIp,
        String idempotencyKey
) {}
