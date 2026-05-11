package com.example.couponservice.domain.model.page;

import java.util.List;

public record Page<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {
    public Page {
        if (content == null) throw new IllegalArgumentException("content must not be null");
        content = List.copyOf(content);
    }

    public int getTotalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
    }
}
