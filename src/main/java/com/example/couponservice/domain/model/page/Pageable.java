package com.example.couponservice.domain.model.page;

public record Pageable(int page, int size) {
    public Pageable {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1) throw new IllegalArgumentException("size must be at least 1");
    }
}
