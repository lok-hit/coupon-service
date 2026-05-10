package com.example.couponservice.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequest(
        @NotBlank(message = "userId must not be blank")
        String userId
) {}
