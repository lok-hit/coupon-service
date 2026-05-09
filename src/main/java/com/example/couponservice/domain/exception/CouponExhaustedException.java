package com.example.couponservice.domain.exception;

public class CouponExhaustedException extends RuntimeException {
    public CouponExhaustedException(String code) {
        super("Coupon exhausted (max uses reached): " + code);
    }
}
