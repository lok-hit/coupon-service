package com.example.couponservice.domain.port.in;

import com.example.couponservice.domain.model.Coupon;

import java.time.LocalDateTime;

public interface CreateCouponUseCase {

    record Command(
            String code,
            String country,
            int maxUses,
            LocalDateTime validUntil
    ) {
        public Command {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
            if (code.length() > 50) throw new IllegalArgumentException("code must not exceed 50 characters");
            code = code.toUpperCase();
            if (!code.matches("[A-Z0-9]+")) throw new IllegalArgumentException("code must be alphanumeric");
            if (country == null || !country.matches("[A-Z]{2}")) throw new IllegalArgumentException("country must be ISO 3166-1 alpha-2 uppercase");
            if (maxUses < 1) throw new IllegalArgumentException("maxUses must be at least 1");
        }
    }

    Coupon create(Command command);
}
