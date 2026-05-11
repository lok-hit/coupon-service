package com.example.couponservice.domain.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Coupon not found: " + code);
    }
}
