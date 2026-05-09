package com.example.couponservice.domain.service;

import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.in.GetCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;

// Thread-safe: stateless — no shared mutable fields; all state flows through method parameters.
public class GetCouponService implements GetCouponUseCase {

    private final CouponRepository couponRepository;

    public GetCouponService(CouponRepository couponRepository) {
        if (couponRepository == null) throw new IllegalArgumentException("couponRepository must not be null");
        this.couponRepository = couponRepository;
    }

    // Thread-safe: delegates to repository; no local mutable state.
    @Override
    public Coupon getByCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        return couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CouponNotFoundException(code));
    }

    // Thread-safe: delegates to repository; no local mutable state.
    @Override
    public Page<Coupon> getAll(Pageable pageable) {
        if (pageable == null) throw new IllegalArgumentException("pageable must not be null");
        return couponRepository.findAll(pageable);
    }
}
