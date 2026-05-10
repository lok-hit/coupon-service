package com.example.couponservice.infrastructure.web;

import com.example.couponservice.domain.exception.CountryNotAllowedException;
import com.example.couponservice.domain.exception.CouponAlreadyUsedException;
import com.example.couponservice.domain.exception.CouponExhaustedException;
import com.example.couponservice.domain.exception.CouponNotActiveException;
import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.exception.GeoLocationUnavailableException;
import com.example.couponservice.infrastructure.web.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(CouponNotFoundException e) {
        return new ErrorResponse("NOT_FOUND", e.getMessage(), 404);
    }

    @ExceptionHandler({CouponNotActiveException.class, CouponExhaustedException.class, CouponAlreadyUsedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException e) {
        return new ErrorResponse("CONFLICT", e.getMessage(), 409);
    }

    @ExceptionHandler(CountryNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(CountryNotAllowedException e) {
        return new ErrorResponse("FORBIDDEN", e.getMessage(), 403);
    }

    @ExceptionHandler(GeoLocationUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(GeoLocationUnavailableException e) {
        return new ErrorResponse("SERVICE_UNAVAILABLE", e.getMessage(), 503);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException e) {
        return new ErrorResponse("BAD_REQUEST", e.getMessage(), 400);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        var message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorResponse("VALIDATION_ERROR", message, 400);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingHeader(MissingRequestHeaderException e) {
        return new ErrorResponse("BAD_REQUEST", "Missing required header: " + e.getHeaderName(), 400);
    }
}
