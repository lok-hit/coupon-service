package com.example.couponservice.domain.service;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;

import java.time.LocalDateTime;
import java.util.UUID;

// Thread-safe: stateless — all state flows through method parameters and local variables.
public class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository couponRepository;

    public CreateCouponService(CouponRepository couponRepository) {
        if (couponRepository == null) throw new IllegalArgumentException("couponRepository must not be null");
        this.couponRepository = couponRepository;
    }

    // Thread-safe: no shared mutable state; Coupon is constructed immutably per call.
    @Override
    public Coupon create(Command command) {
        if (command == null) throw new IllegalArgumentException("command must not be null");

        Coupon coupon = new Coupon(
                UUID.randomUUID(),
                command.code(),
                command.country(),
                command.maxUses(),
                0,
                Coupon.Status.ACTIVE,
                command.validUntil(),
                LocalDateTime.now()
        );

        return couponRepository.save(coupon);
    }
}
