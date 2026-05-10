package com.example.couponservice.infrastructure.persistence.mapper;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.infrastructure.persistence.entity.CouponEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CouponMapperTest {

    private final CouponMapper mapper = new CouponMapperImpl();

    @Test
    void toDomain_allFieldsMappedCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime validUntil = LocalDateTime.now().plusDays(30);
        LocalDateTime createdAt = LocalDateTime.now();

        CouponEntity entity = CouponEntity.builder()
                .id(id)
                .code("SUMMER10")
                .country("PL")
                .maxUses(100)
                .currentUses(5)
                .status(Coupon.Status.ACTIVE)
                .validUntil(validUntil)
                .createdAt(createdAt)
                .version(1L)
                .build();

        Coupon coupon = mapper.toDomain(entity);

        assertThat(coupon.getId()).isEqualTo(id);
        assertThat(coupon.getCode()).isEqualTo("SUMMER10");
        assertThat(coupon.getCountry()).isEqualTo("PL");
        assertThat(coupon.getMaxUses()).isEqualTo(100);
        assertThat(coupon.getCurrentUses()).isEqualTo(5);
        assertThat(coupon.getStatus()).isEqualTo(Coupon.Status.ACTIVE);
        assertThat(coupon.getValidUntil()).isEqualTo(validUntil);
        assertThat(coupon.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void toDomain_disabledStatus_mappedCorrectly() {
        CouponEntity entity = CouponEntity.builder()
                .id(UUID.randomUUID())
                .code("DISABLED1")
                .country("DE")
                .maxUses(10)
                .currentUses(0)
                .status(Coupon.Status.DISABLED)
                .validUntil(null)
                .createdAt(LocalDateTime.now())
                .build();

        Coupon coupon = mapper.toDomain(entity);

        assertThat(coupon.getStatus()).isEqualTo(Coupon.Status.DISABLED);
    }

    @Test
    void toDomain_nullInput_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toEntity_allFieldsMappedCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        Coupon coupon = new Coupon(id, "WIOSNA2025", "PL", 50, 3,
                Coupon.Status.ACTIVE, null, createdAt);

        CouponEntity entity = mapper.toEntity(coupon);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCode()).isEqualTo("WIOSNA2025");
        assertThat(entity.getCountry()).isEqualTo("PL");
        assertThat(entity.getMaxUses()).isEqualTo(50);
        assertThat(entity.getCurrentUses()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(Coupon.Status.ACTIVE);
        assertThat(entity.getValidUntil()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getVersion()).isNull();
    }
}