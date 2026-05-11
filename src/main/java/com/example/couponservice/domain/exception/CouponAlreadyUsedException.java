package com.example.couponservice.domain.exception;

public class CouponAlreadyUsedException extends RuntimeException {
    public CouponAlreadyUsedException(String userId, String code) {
        super("User '" + userId + "' has already used coupon: " + code);
    }
}
