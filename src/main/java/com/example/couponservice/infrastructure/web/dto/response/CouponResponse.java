package com.example.couponservice.infrastructure.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

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
