package com.example.couponservice.domain.exception;

public class CouponNotActiveException extends RuntimeException {
    public CouponNotActiveException(String code) {
        super("Coupon is not active or has expired: " + code);
    }
}
