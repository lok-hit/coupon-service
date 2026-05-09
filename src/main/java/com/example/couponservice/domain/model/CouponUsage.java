package com.example.couponservice.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

public record CouponUsage(
        UUID id,
        String couponCode,
        String userId,
        LocalDateTime usedAt,
        String sourceIp,
        String idempotencyKey
) {
    private static final Pattern IPV4 = Pattern.compile(
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.\\d{1,3}$"
    );

    public CouponUsage {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (couponCode == null || couponCode.isBlank()) throw new IllegalArgumentException("couponCode must not be blank");
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId must not be blank");
        if (usedAt == null) throw new IllegalArgumentException("usedAt must not be null");
        if (sourceIp == null || sourceIp.isBlank()) throw new IllegalArgumentException("sourceIp must not be blank");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotencyKey must not be blank");
    }

    // Zeros the last octet for GDPR compliance, e.g. 192.168.1.123 -> 192.168.1.0
    public static String anonymizeIp(String ip) {
        if (ip == null) return "0.0.0.0";
        var matcher = IPV4.matcher(ip.trim());
        if (matcher.matches()) {
            return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) + ".0";
        }
        return "0.0.0.0";
    }
}
