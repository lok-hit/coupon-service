package com.example.couponservice.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for redeeming a coupon.
 */
public record RedeemCouponRequest(
        @NotBlank(message = "userId must not be blank")
        @Size(max = 255, message = "userId must not exceed 255 characters")
        String userId
) {}
