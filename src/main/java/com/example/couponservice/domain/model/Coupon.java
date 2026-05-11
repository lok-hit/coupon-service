package com.example.couponservice.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Coupon {

    public enum Status {
        ACTIVE, DISABLED
    }

    private final UUID id;
    private final String code;
    private final String country;
    private final int maxUses;
    private final int currentUses;
    private final Status status;
    private final LocalDateTime validUntil;
    private final LocalDateTime createdAt;

    public Coupon(UUID id, String code, String country, int maxUses, int currentUses,
                  Status status, LocalDateTime validUntil, LocalDateTime createdAt) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (code.length() > 50) throw new IllegalArgumentException("code must not exceed 50 characters");
        if (!code.matches("[A-Z0-9]+")) throw new IllegalArgumentException("code must be alphanumeric uppercase");
        if (country == null || !country.matches("[A-Z]{2}")) throw new IllegalArgumentException("country must be ISO 3166-1 alpha-2");
        if (maxUses < 1) throw new IllegalArgumentException("maxUses must be at least 1");
        if (currentUses < 0) throw new IllegalArgumentException("currentUses must not be negative");
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");

        this.id = id;
        this.code = code;
        this.country = country;
        this.maxUses = maxUses;
        this.currentUses = currentUses;
        this.status = status;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
    }

    // Thread-safe: checks only immutable fields and a parameter — no shared mutable state
    public boolean isActive(LocalDateTime now) {
        if (status != Status.ACTIVE) return false;
        if (validUntil != null && !now.isBefore(validUntil)) return false;
        return true;
    }

    public boolean isCountryAllowed(String countryCode) {
        return this.country.equalsIgnoreCase(countryCode);
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getCountry() { return country; }
    public int getMaxUses() { return maxUses; }
    public int getCurrentUses() { return currentUses; }
    public Status getStatus() { return status; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
