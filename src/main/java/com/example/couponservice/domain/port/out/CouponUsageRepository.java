package com.example.couponservice.domain.port.out;

import com.example.couponservice.domain.model.CouponUsage;

import java.util.Optional;

public interface CouponUsageRepository {

    boolean existsByUserIdAndCouponCode(String userId, String couponCode);

    Optional<CouponUsage> findByIdempotencyKey(String idempotencyKey);

    CouponUsage save(CouponUsage usage);
}
