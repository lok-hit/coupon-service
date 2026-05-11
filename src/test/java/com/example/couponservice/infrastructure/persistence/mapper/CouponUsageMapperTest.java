package com.example.couponservice.infrastructure.persistence.mapper;

import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.infrastructure.persistence.entity.CouponUsageEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CouponUsageMapperTest {

    private final CouponUsageMapper mapper = new CouponUsageMapperImpl();

    @Test
    void toDomain_allFieldsMappedCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime usedAt = LocalDateTime.now();

        CouponUsageEntity entity = CouponUsageEntity.builder()
                .id(id)
                .couponCode("SUMMER10")
                .userId("user-123")
                .usedAt(usedAt)
                .sourceIp("192.168.1.0")
                .idempotencyKey("idem-key-abc")
                .build();

        CouponUsage usage = mapper.toDomain(entity);

        assertThat(usage.id()).isEqualTo(id);
        assertThat(usage.couponCode()).isEqualTo("SUMMER10");
        assertThat(usage.userId()).isEqualTo("user-123");
        assertThat(usage.usedAt()).isEqualTo(usedAt);
        assertThat(usage.sourceIp()).isEqualTo("192.168.1.0");
        assertThat(usage.idempotencyKey()).isEqualTo("idem-key-abc");
    }

    @Test
    void toEntity_allFieldsMappedCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime usedAt = LocalDateTime.now();

        CouponUsage usage = new CouponUsage(id, "WIOSNA2025", "user-456",
                usedAt, "10.0.0.0", "idem-xyz-789");

        CouponUsageEntity entity = mapper.toEntity(usage);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCouponCode()).isEqualTo("WIOSNA2025");
        assertThat(entity.getUserId()).isEqualTo("user-456");
        assertThat(entity.getUsedAt()).isEqualTo(usedAt);
        assertThat(entity.getSourceIp()).isEqualTo("10.0.0.0");
        assertThat(entity.getIdempotencyKey()).isEqualTo("idem-xyz-789");
    }
}