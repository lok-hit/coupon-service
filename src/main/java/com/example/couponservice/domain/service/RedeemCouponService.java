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
import java.util.concurrent.StructuredTaskScope;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
        Coupon coupon = couponRepository.findByCode(command.couponCode())
            .orElseThrow(() -> new CouponNotFoundException(command.couponCode()));

        if (!coupon.isActive(LocalDateTime.now())) {
            throw new CouponNotActiveException(command.couponCode());
        }

        Optional<CouponUsage> existing = couponUsageRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        ValidationResult validation = runConcurrentValidation(command);

        if (validation.alreadyUsed()) {
            throw new CouponAlreadyUsedException(command.userId(), command.couponCode());
        }
        if (!coupon.isCountryAllowed(validation.detectedCountry())) {
            throw new CountryNotAllowedException(validation.detectedCountry(), coupon.getCountry());
        }

        int rowsAffected = couponRepository.incrementUsage(command.couponCode());
        if (rowsAffected == 0) {
            throw new CouponExhaustedException(command.couponCode());
        }

        return couponUsageRepository.save(buildUsage(command));
    }

    private record ValidationResult(boolean alreadyUsed, String detectedCountry) {}

    private ValidationResult runConcurrentValidation(Command command) {
        try (var scope = StructuredTaskScope.open()) {
            var usageCheckTask = scope.fork(() ->
                couponUsageRepository.existsByUserIdAndCouponCode(command.userId(), command.couponCode())
            );
            var geoTask = scope.fork(() ->
                geoLocationPort.getCountryCode(command.sourceIp())
            );

            scope.join();

            if (usageCheckTask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                Throwable cause = usageCheckTask.exception();
                if (cause instanceof RuntimeException re) throw re;
                throw new IllegalStateException("Usage check failed", cause);
            }
            if (geoTask.state() == StructuredTaskScope.Subtask.State.FAILED) {
                Throwable cause = geoTask.exception();
                if (cause instanceof RuntimeException re) throw re;
                throw new IllegalStateException("Geo-location failed", cause);
            }

            String detectedCountry = geoTask.get()
                .orElseThrow(() -> new GeoLocationUnavailableException(command.sourceIp()));

            return new ValidationResult(usageCheckTask.get(), detectedCountry);

        } catch (GeoLocationUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Redeem interrupted during concurrent validation", e);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof GeoLocationUnavailableException gue) throw gue;
            if (cause instanceof RuntimeException re) throw re;
            throw new IllegalStateException("Unexpected failure during concurrent validation", cause);
        }
    }

    private CouponUsage buildUsage(Command command) {
        return new CouponUsage(
            UUID.randomUUID(),
            command.couponCode(),
            command.userId(),
            LocalDateTime.now(),
            CouponUsage.anonymizeIp(command.sourceIp()),
            command.idempotencyKey()
        );
    }
}
