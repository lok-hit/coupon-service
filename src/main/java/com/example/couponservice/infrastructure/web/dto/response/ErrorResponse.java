package com.example.couponservice.infrastructure.web.dto.response;

public record ErrorResponse(
        String error,
        String message,
        int status
) {}
