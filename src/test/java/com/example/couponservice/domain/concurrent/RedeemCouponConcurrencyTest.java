package com.example.couponservice.domain.concurrent;

import com.example.couponservice.domain.exception.CouponExhaustedException;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;
import com.example.couponservice.domain.port.out.CouponUsageRepository;
import com.example.couponservice.domain.port.out.GeoLocationPort;
import com.example.couponservice.domain.service.RedeemCouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedeemCouponConcurrencyTest {

    private static final String CODE = "HOTDEAL";
    private static final String COUNTRY = "PL";
    private static final int MAX_USES = 1;
    private static final int THREADS = 50;

    private FakeCouponRepository fakeCouponRepository;
    private FakeCouponUsageRepository fakeCouponUsageRepository;
    private FakeGeoLocationPort fakeGeoLocationPort;
    private RedeemCouponService service;

    @BeforeEach
    void setUp() {
        Coupon coupon = new Coupon(UUID.randomUUID(), CODE, COUNTRY, MAX_USES, 0,
                Coupon.Status.ACTIVE, LocalDateTime.now().plusDays(1), LocalDateTime.now());

        fakeCouponRepository = new FakeCouponRepository(coupon);
        fakeCouponUsageRepository = new FakeCouponUsageRepository();
        fakeGeoLocationPort = new FakeGeoLocationPort(COUNTRY);
        service = new RedeemCouponService(fakeCouponRepository, fakeCouponUsageRepository, fakeGeoLocationPort);
    }

    @Test
    void fiftyVirtualThreadsOnMaxUsesOne_exactlyOneSucceeds() throws InterruptedException {
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger exhaustedFailures = new AtomicInteger(0);

        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        List<Thread> threads = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final String userId = "user-" + i;
            final String idempotencyKey = "idem-" + i;
            final String sourceIp = "10.0.0." + (i % 254 + 1);
            Thread t = Thread.ofVirtual().unstarted(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    RedeemCouponUseCase.Command cmd = new RedeemCouponUseCase.Command(
                            CODE, userId, sourceIp, idempotencyKey);
                    service.redeem(cmd);
                    successes.incrementAndGet();
                } catch (CouponExhaustedException e) {
                    exhaustedFailures.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
            threads.add(t);
        }

        threads.forEach(Thread::start);
        ready.await();   // wait until all threads are ready
        start.countDown(); // release all threads simultaneously
        done.await();    // wait until all threads finish

        assertEquals(1, successes.get(), "Exactly one thread must succeed");
        assertEquals(THREADS - 1, exhaustedFailures.get(), "All other threads must get CouponExhaustedException");
        assertEquals(THREADS, fakeCouponRepository.incrementUsageCallCount(),
                "incrementUsage must be called exactly N times (CAS attempted by every thread)");
    }

    // -------------------------------------------------------------------------
    // In-memory fake implementations — thread-safe, no Mockito
    // -------------------------------------------------------------------------

    static class FakeCouponRepository implements CouponRepository {

        private final Coupon coupon;
        // current usage counter; CAS increments only when currentValue < maxUses
        private final AtomicInteger currentUses = new AtomicInteger(0);
        private final AtomicInteger incrementCalls = new AtomicInteger(0);

        FakeCouponRepository(Coupon coupon) {
            this.coupon = coupon;
        }

        @Override
        public Optional<Coupon> findByCode(String code) {
            if (coupon.getCode().equals(code)) return Optional.of(coupon);
            return Optional.empty();
        }

        @Override
        public Coupon save(Coupon c) {
            return c;
        }

        @Override
        public Page<Coupon> findAll(Pageable pageable) {
            return new Page<>(List.of(coupon), pageable.page(), pageable.size(), 1L);
        }

        /*
         * Thread-safe CAS: atomically increments only when currentUses < maxUses.
         * compareAndSet retries until it either wins the slot or confirms exhaustion.
         * This mirrors the SQL: UPDATE coupons SET current_uses = current_uses + 1
         *                       WHERE code = ? AND current_uses < max_uses
         */
        @Override
        public int incrementUsage(String code) {
            incrementCalls.incrementAndGet();
            int maxUses = coupon.getMaxUses();
            int current;
            do {
                current = currentUses.get();
                if (current >= maxUses) return 0;
            } while (!currentUses.compareAndSet(current, current + 1));
            return 1;
        }

        int incrementUsageCallCount() {
            return incrementCalls.get();
        }
    }

    static class FakeCouponUsageRepository implements CouponUsageRepository {

        // key: userId + "|" + couponCode
        private final Map<String, Boolean> usageByUserAndCode = new ConcurrentHashMap<>();
        private final Map<String, CouponUsage> usageByIdempotencyKey = new ConcurrentHashMap<>();

        @Override
        public boolean existsByUserIdAndCouponCode(String userId, String couponCode) {
            return usageByUserAndCode.containsKey(userId + "|" + couponCode);
        }

        @Override
        public Optional<CouponUsage> findByIdempotencyKey(String idempotencyKey) {
            return Optional.ofNullable(usageByIdempotencyKey.get(idempotencyKey));
        }

        @Override
        public CouponUsage save(CouponUsage usage) {
            usageByUserAndCode.put(usage.userId() + "|" + usage.couponCode(), Boolean.TRUE);
            usageByIdempotencyKey.put(usage.idempotencyKey(), usage);
            return usage;
        }
    }

    static class FakeGeoLocationPort implements GeoLocationPort {

        private final String fixedCountryCode;

        FakeGeoLocationPort(String fixedCountryCode) {
            this.fixedCountryCode = fixedCountryCode;
        }

        @Override
        public Optional<String> getCountryCode(String ip) {
            return Optional.of(fixedCountryCode);
        }
    }
}
