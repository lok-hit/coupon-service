package com.example.couponservice.api.exception;

import com.example.couponservice.api.dto.response.ErrorResponse;
import com.example.couponservice.domain.exception.CountryNotAllowedException;
import com.example.couponservice.domain.exception.CouponAlreadyUsedException;
import com.example.couponservice.domain.exception.CouponExhaustedException;
import com.example.couponservice.domain.exception.CouponNotActiveException;
import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.exception.GeoLocationUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates domain and validation exceptions into structured HTTP error responses.
 * No stack traces are ever included in response bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Handles requests for coupons that do not exist. */
    @ExceptionHandler(CouponNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(CouponNotFoundException e) {
        log.warn("Coupon not found: {}", e.getMessage());
        return new ErrorResponse("NOT_FOUND", e.getMessage());
    }

    /** Handles attempts to redeem a coupon that is not in ACTIVE state or is expired. */
    @ExceptionHandler(CouponNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleNotActive(CouponNotActiveException e) {
        log.warn("Coupon not active: {}", e.getMessage());
        return new ErrorResponse("COUPON_NOT_ACTIVE", e.getMessage());
    }

    /** Handles attempts to redeem a coupon that has reached its maximum usage limit. */
    @ExceptionHandler(CouponExhaustedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleExhausted(CouponExhaustedException e) {
        log.warn("Coupon exhausted: {}", e.getMessage());
        return new ErrorResponse("COUPON_EXHAUSTED", e.getMessage());
    }

    /** Handles attempts by a user to redeem a coupon they have already used. */
    @ExceptionHandler(CouponAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyUsed(CouponAlreadyUsedException e) {
        log.warn("Coupon already used: {}", e.getMessage());
        return new ErrorResponse("COUPON_ALREADY_USED", e.getMessage());
    }

    /** Handles redemption attempts from a country not allowed by the coupon's geo-restriction. */
    @ExceptionHandler(CountryNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleCountryNotAllowed(CountryNotAllowedException e) {
        log.warn("Country not allowed: {}", e.getMessage());
        return new ErrorResponse("COUNTRY_NOT_ALLOWED", e.getMessage());
    }

    /** Handles failures in the geo-location service. */
    @ExceptionHandler(GeoLocationUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleGeoLocationUnavailable(GeoLocationUnavailableException e) {
        log.warn("Geo-location unavailable: {}", e.getMessage());
        return new ErrorResponse("GEO_LOCATION_UNAVAILABLE", e.getMessage());
    }

    /** Handles Bean Validation failures on request bodies or parameters. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    /** Catch-all handler that prevents leaking internal error details to the client. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Please try again later.");
    }
}
