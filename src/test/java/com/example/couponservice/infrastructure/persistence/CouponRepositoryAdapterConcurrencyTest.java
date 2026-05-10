package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.domain.model.Coupon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency integration test proving CAS atomicity under real concurrent load.
 *
 * <p>50 virtual threads race to redeem a coupon with maxUses=1 against a live
 * PostgreSQL instance. Exactly 1 must succeed (rowsAffected=1); all others must
 * see 0 — no exceptions, no over-redemption.
 *
 * <p>This is the most important test in the infrastructure suite: it verifies that
 * the single-statement CAS UPDATE prevents double-spending without application-level locking.
 */
@SpringBootTest
@Testcontainers
class CouponRepositoryAdapterConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    /** Prevents Spring from attempting a Redis connection during context startup. */
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("geolocation.provider-url", () -> "http://localhost");
        registry.add("geolocation.connect-timeout", () -> "1s");
        registry.add("geolocation.read-timeout", () -> "1s");
        registry.add("geolocation.cache-ttl", () -> "1h");
        registry.add("geolocation.strategy", () -> "fail-open");
    }

    @Autowired
    private CouponRepositoryAdapter couponRepositoryAdapter;

    @Test
    void incrementUsage_fiftyVirtualThreadsConcurrently_exactlyOneSucceeds() throws InterruptedException {
        int threadCount = 50;
        String code = "CONCUR001";

        Coupon coupon = new Coupon(
                UUID.randomUUID(), code, "PL", 1, 0,
                Coupon.Status.ACTIVE, null, LocalDateTime.now());
        couponRepositoryAdapter.save(coupon);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    int rows = couponRepositoryAdapter.incrementUsage(code);
                    if (rows == 1) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        assertThat(completed).as("All threads should finish within 30 seconds").isTrue();
        assertThat(successCount.get())
                .as("Exactly one thread should succeed (CAS atomicity)")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("All other threads should see 0 rows affected")
                .isEqualTo(threadCount - 1);

        Coupon updated = couponRepositoryAdapter.findByCode(code).orElseThrow();
        assertThat(updated.getCurrentUses())
                .as("Database currentUses must be exactly 1 after all threads complete")
                .isEqualTo(1);
    }
}