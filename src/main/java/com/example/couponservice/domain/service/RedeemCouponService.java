package com.example.couponservice.domain.service;

import com.example.couponservice.domain.exception.CouponAlreadyUsedException;
import com.example.couponservice.domain.exception.CouponExhaustedException;
import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.exception.CouponNotActiveException;
import com.example.couponservice.domain.exception.CountryNotAllowedException;
import com.example.couponservice.domain.exception.GeoLocationUnavailableException;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;
import com.example.couponservice.domain.port.out.CouponUsageRepository;
import com.example.couponservice.domain.port.out.GeoLocationPort;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.StructuredTaskScope;

/*
 * Thread-safe by design:
 *  - No shared mutable fields (couponRepository, couponUsageRepository, geoLocationPort are all interfaces
 *    assumed to be thread-safe themselves, as required of port implementations).
 *  - All execution state is local to the calling virtual thread's stack.
 *  - CAS safety against concurrent over-redemption is delegated to CouponRepository.incrementUsage.
 *  - Steps 4 (userId check) and 5 (geo-location) are independent and run concurrently inside a
 *    StructuredTaskScope.ShutdownOnFailure so that either failure cancels the other immediately.
 */
public class RedeemCouponService implements RedeemCouponUseCase {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final GeoLocationPort geoLocationPort;

    public RedeemCouponService(CouponRepository couponRepository,
                                CouponUsageRepository couponUsageRepository,
                                GeoLocationPort geoLocationPort) {
        if (couponRepository == null) throw new IllegalArgumentException("couponRepository must not be null");
        if (couponUsageRepository == null) throw new IllegalArgumentException("couponUsageRepository must not be null");
        if (geoLocationPort == null) throw new IllegalArgumentException("geoLocationPort must not be null");
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.geoLocationPort = geoLocationPort;
    }

    /*
     * Thread-safe: every local variable is confined to the calling virtual thread's stack frame.
     * The StructuredTaskScope is created and closed within this single invocation — no scope or
     * subtask state escapes the method boundary.
     */
    @Override
    public CouponUsage redeem(Command command) {
        // Step 1 — cheapest lookup first
        Coupon coupon = couponRepository.findByCode(command.couponCode())
                .orElseThrow(() -> new CouponNotFoundException(command.couponCode()));

        // Step 2 — status + expiry check; no I/O
        if (!coupon.isActive(LocalDateTime.now())) {
            throw new CouponNotActiveException(command.couponCode());
        }

        // Step 3 — idempotency: if this key already produced a result, return it
        Optional<CouponUsage> existing = couponUsageRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Steps 4 & 5 — run concurrently; ShutdownOnFailure cancels the peer on first exception
        boolean alreadyUsed;
        String detectedCountry;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Step 4: check if this userId already redeemed the coupon
            StructuredTaskScope.Subtask<Boolean> usageCheckTask = scope.fork(() ->
                    couponUsageRepository.existsByUserIdAndCouponCode(command.userId(), command.couponCode())
            );

            // Step 5: resolve geo-location for the source IP
            StructuredTaskScope.Subtask<Optional<String>> geoTask = scope.fork(() ->
                    geoLocationPort.getCountryCode(command.sourceIp())
            );

            scope.join();           // wait for both (or first failure)
            scope.throwIfFailed();  // re-throws the first subtask exception wrapped in ExecutionException

            alreadyUsed = usageCheckTask.get();
            detectedCountry = geoTask.get()
                    .orElseThrow(() -> new GeoLocationUnavailableException(command.sourceIp()));

        } catch (GeoLocationUnavailableException | CouponAlreadyUsedException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Redeem interrupted while checking usage and geo-location", e);
        } catch (Exception e) {
            // Unwrap domain exceptions propagated through ExecutionException
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof GeoLocationUnavailableException gue) throw gue;
            if (cause instanceof RuntimeException re) throw re;
            throw new IllegalStateException("Unexpected failure during concurrent validation", cause);
        }

        // Step 4 result evaluated after scope closes
        if (alreadyUsed) {
            throw new CouponAlreadyUsedException(command.userId(), command.couponCode());
        }

        // Step 5 result evaluated after scope closes
        if (!coupon.isCountryAllowed(detectedCountry)) {
            throw new CountryNotAllowedException(detectedCountry, coupon.getCountry());
        }

        // Step 6 — CAS; 0 rows means another thread beat us to the last slot
        int rowsAffected = couponRepository.incrementUsage(command.couponCode());
        if (rowsAffected == 0) {
            throw new CouponExhaustedException(command.couponCode());
        }

        // Step 7 — persist usage record
        String anonymizedIp = CouponUsage.anonymizeIp(command.sourceIp());
        CouponUsage usage = new CouponUsage(
                UUID.randomUUID(),
                command.couponCode(),
                command.userId(),
                LocalDateTime.now(),
                anonymizedIp,
                command.idempotencyKey()
        );
        return couponUsageRepository.save(usage);
    }
}
