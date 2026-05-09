package com.example.couponservice.domain.port.in;

import com.example.couponservice.domain.model.CouponUsage;

public interface RedeemCouponUseCase {

    record Command(
            String couponCode,
            String userId,
            String sourceIp,
            String idempotencyKey
    ) {
        public Command {
            if (couponCode == null || couponCode.isBlank()) throw new IllegalArgumentException("couponCode must not be blank");
            if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId must not be blank");
            if (sourceIp == null || sourceIp.isBlank()) throw new IllegalArgumentException("sourceIp must not be blank");
            if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotencyKey must not be blank");
            couponCode = couponCode.toUpperCase();
        }
    }

    CouponUsage redeem(Command command);
}
