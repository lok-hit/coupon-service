package com.example.couponservice.domain.exception;

public class CountryNotAllowedException extends RuntimeException {
    public CountryNotAllowedException(String detectedCountry, String requiredCountry) {
        super("Country '" + detectedCountry + "' is not allowed for this coupon; required: " + requiredCountry);
    }
}
