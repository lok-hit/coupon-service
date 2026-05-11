package com.example.couponservice.api.dto.response;

import java.time.LocalDateTime;

/**
 * Standard error response payload returned for all API error conditions.
 */
public record ErrorResponse(String code, String message, LocalDateTime timestamp) {

    /** Constructs an {@code ErrorResponse} with the current timestamp. */
    public ErrorResponse(String code, String message) {
        this(code, message, LocalDateTime.now());
    }
}
