package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.infrastructure.persistence.mapper.CouponMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CouponRepositoryAdapter} against a real PostgreSQL database
 * via Testcontainers, with Flyway migrations applied.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlywayAutoConfiguration.class, CouponRepositoryAdapter.class, CouponMapperImpl.class})
@Testcontainers
class CouponRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private CouponRepositoryAdapter adapter;

    private Coupon buildCoupon(String code) {
        return new Coupon(
                UUID.randomUUID(), code, "PL", 10, 0,
                Coupon.Status.ACTIVE, null, LocalDateTime.now());
    }

    @Test
    void save_persistsAndReturnsDomainObject() {
        Coupon coupon = buildCoupon("SAVETEST1");

        Coupon saved = adapter.save(coupon);

        assertThat(saved.getId()).isEqualTo(coupon.getId());
        assertThat(saved.getCode()).isEqualTo("SAVETEST1");
        assertThat(saved.getCountry()).isEqualTo("PL");
        assertThat(saved.getMaxUses()).isEqualTo(10);
        assertThat(saved.getCurrentUses()).isZero();
    }

    @Test
    void findByCode_caseInsensitive_found() {
        adapter.save(buildCoupon("WIOSNA"));

        Optional<Coupon> result = adapter.findByCode("wiosna");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("WIOSNA");
    }

    @Test
    void findByCode_unknownCode_returnsEmpty() {
        Optional<Coupon> result = adapter.findByCode("DOESNOTEXIST");
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedResults() {
        adapter.save(buildCoupon("PAGEA0001"));
        adapter.save(buildCoupon("PAGEB0002"));
        adapter.save(buildCoupon("PAGEC0003"));

        Page<Coupon> page = adapter.findAll(new Pageable(0, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page.pageNumber()).isZero();
        assertThat(page.pageSize()).isEqualTo(2);
    }

    @Test
    void incrementUsage_happyPath_returnsOne() {
        adapter.save(buildCoupon("INCR00001"));

        int rowsAffected = adapter.incrementUsage("INCR00001");

        assertThat(rowsAffected).isEqualTo(1);
    }

    @Test
    void incrementUsage_exhausted_returnsZero() {
        // maxUses=1, currentUses=1 → already at the limit
        Coupon exhausted = new Coupon(
                UUID.randomUUID(), "EXHAUS001", "PL", 1, 1,
                Coupon.Status.ACTIVE, null, LocalDateTime.now());
        adapter.save(exhausted);

        int rowsAffected = adapter.incrementUsage("EXHAUS001");

        assertThat(rowsAffected).isZero();
    }
}