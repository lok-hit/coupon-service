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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedeemCouponServiceTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponUsageRepository couponUsageRepository;
    @Mock GeoLocationPort geoLocationPort;

    RedeemCouponService service;

    private static final String CODE = "SAVE20";
    private static final String USER_ID = "user-123";
    private static final String SOURCE_IP = "192.168.1.55";
    private static final String IDEMPOTENCY_KEY = "idem-abc-123";
    private static final String COUNTRY = "PL";

    @BeforeEach
    void setUp() {
        service = new RedeemCouponService(couponRepository, couponUsageRepository, geoLocationPort);
    }

    private RedeemCouponUseCase.Command command() {
        return new RedeemCouponUseCase.Command(CODE, USER_ID, SOURCE_IP, IDEMPOTENCY_KEY);
    }

    private Coupon activeCoupon() {
        return new Coupon(UUID.randomUUID(), CODE, COUNTRY, 10, 0,
                Coupon.Status.ACTIVE, LocalDateTime.now().plusDays(30), LocalDateTime.now());
    }

    @Test
    void step1_couponNotFound_throwsCouponNotFoundException() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.empty());

        CouponNotFoundException ex = assertThrows(CouponNotFoundException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains(CODE));
        verify(couponUsageRepository, never()).existsByUserIdAndCouponCode(anyString(), anyString());
    }

    @Test
    void step2_couponDisabled_throwsCouponNotActiveException() {
        Coupon disabled = new Coupon(UUID.randomUUID(), CODE, COUNTRY, 10, 0,
                Coupon.Status.DISABLED, null, LocalDateTime.now());
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(disabled));

        CouponNotActiveException ex = assertThrows(CouponNotActiveException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains(CODE));
    }

    @Test
    void step2_couponExpired_throwsCouponNotActiveException() {
        Coupon expired = new Coupon(UUID.randomUUID(), CODE, COUNTRY, 10, 0,
                Coupon.Status.ACTIVE, LocalDateTime.now().minusSeconds(1), LocalDateTime.now().minusDays(1));
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(expired));

        assertThrows(CouponNotActiveException.class, () -> service.redeem(command()));
    }

    @Test
    void step3_idempotencyKeyAlreadyExists_returnsPreviousResult() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        CouponUsage previousUsage = new CouponUsage(UUID.randomUUID(), CODE, USER_ID,
                LocalDateTime.now().minusMinutes(5), "192.168.1.0", IDEMPOTENCY_KEY);
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(previousUsage));

        CouponUsage result = service.redeem(command());

        assertSame(previousUsage, result);
        verify(couponRepository, never()).incrementUsage(anyString());
    }

    @Test
    void step4_userAlreadyUsedCoupon_throwsCouponAlreadyUsedException() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(couponUsageRepository.existsByUserIdAndCouponCode(USER_ID, CODE)).thenReturn(true);
        when(geoLocationPort.getCountryCode(SOURCE_IP)).thenReturn(Optional.of(COUNTRY));

        CouponAlreadyUsedException ex = assertThrows(CouponAlreadyUsedException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains(USER_ID));
        assertTrue(ex.getMessage().contains(CODE));
        verify(couponRepository, never()).incrementUsage(anyString());
    }

    @Test
    void step5_countryMismatch_throwsCountryNotAllowedException() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(couponUsageRepository.existsByUserIdAndCouponCode(USER_ID, CODE)).thenReturn(false);
        when(geoLocationPort.getCountryCode(SOURCE_IP)).thenReturn(Optional.of("DE"));

        CountryNotAllowedException ex = assertThrows(CountryNotAllowedException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains("DE"));
        assertTrue(ex.getMessage().contains(COUNTRY));
        verify(couponRepository, never()).incrementUsage(anyString());
    }

    @Test
    void step5_geoLocationUnavailable_throwsGeoLocationUnavailableException() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(couponUsageRepository.existsByUserIdAndCouponCode(USER_ID, CODE)).thenReturn(false);
        when(geoLocationPort.getCountryCode(SOURCE_IP)).thenReturn(Optional.empty());

        GeoLocationUnavailableException ex = assertThrows(GeoLocationUnavailableException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains(SOURCE_IP));
        verify(couponRepository, never()).incrementUsage(anyString());
    }

    @Test
    void step6_casReturnsZeroRows_throwsCouponExhaustedException() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(couponUsageRepository.existsByUserIdAndCouponCode(USER_ID, CODE)).thenReturn(false);
        when(geoLocationPort.getCountryCode(SOURCE_IP)).thenReturn(Optional.of(COUNTRY));
        when(couponRepository.incrementUsage(CODE)).thenReturn(0);

        CouponExhaustedException ex = assertThrows(CouponExhaustedException.class, () -> service.redeem(command()));

        assertTrue(ex.getMessage().contains(CODE));
    }

    @Test
    void happyPath_savesAndReturnsCouponUsage() {
        when(couponRepository.findByCode(CODE)).thenReturn(Optional.of(activeCoupon()));
        when(couponUsageRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(couponUsageRepository.existsByUserIdAndCouponCode(USER_ID, CODE)).thenReturn(false);
        when(geoLocationPort.getCountryCode(SOURCE_IP)).thenReturn(Optional.of(COUNTRY));
        when(couponRepository.incrementUsage(CODE)).thenReturn(1);

        CouponUsage saved = new CouponUsage(UUID.randomUUID(), CODE, USER_ID,
                LocalDateTime.now(), "192.168.1.0", IDEMPOTENCY_KEY);
        when(couponUsageRepository.save(any(CouponUsage.class))).thenReturn(saved);

        CouponUsage result = service.redeem(command());

        assertNotNull(result);
        verify(couponUsageRepository).save(any(CouponUsage.class));
    }
}
