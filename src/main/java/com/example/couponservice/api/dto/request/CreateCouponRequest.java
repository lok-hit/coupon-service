package com.example.couponservice.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new coupon.
 */
public record CreateCouponRequest(
        @NotBlank(message = "code must not be blank")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "code must be alphanumeric")
        String code,

        @NotBlank(message = "country must not be blank")
        @Size(min = 2, max = 2, message = "country must be exactly 2 characters")
        @Pattern(regexp = "[A-Z]{2}", message = "country must be ISO 3166-1 alpha-2 uppercase")
        String country,

        @NotNull(message = "maxUses must not be null")
        @Min(value = 1, message = "maxUses must be at least 1")
        Integer maxUses,

        LocalDateTime validUntil
) {}
