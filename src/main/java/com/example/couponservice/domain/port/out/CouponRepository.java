package com.example.couponservice.domain.port.out;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;

import java.util.Optional;

public interface CouponRepository {

    Optional<Coupon> findByCode(String code);

    Coupon save(Coupon coupon);

    Page<Coupon> findAll(Pageable pageable);

    // CAS-style update; returns number of rows affected (0 means maxUses already reached)
    int incrementUsage(String code);
}
