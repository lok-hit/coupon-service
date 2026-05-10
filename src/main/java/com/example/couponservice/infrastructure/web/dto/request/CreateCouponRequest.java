package com.example.couponservice.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank(message = "code must not be blank")
        @Size(max = 50, message = "code must not exceed 50 characters")
        @Pattern(regexp = "[A-Za-z0-9]+", message = "code must be alphanumeric")
        String code,

        @NotNull(message = "country must not be null")
        @Pattern(regexp = "[A-Z]{2}", message = "country must be ISO 3166-1 alpha-2 uppercase")
        String country,

        @Min(value = 1, message = "maxUses must be at least 1")
        int maxUses,

        LocalDateTime validUntil
) {}
