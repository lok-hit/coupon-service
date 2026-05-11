package com.example.couponservice.domain.service;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCouponServiceTest {

    @Mock CouponRepository couponRepository;

    CreateCouponService service;

    @BeforeEach
    void setUp() {
        service = new CreateCouponService(couponRepository);
    }

    private Coupon savedCoupon(String code, String country, int maxUses) {
        return new Coupon(UUID.randomUUID(), code, country, maxUses, 0,
                Coupon.Status.ACTIVE, null, LocalDateTime.now());
    }

    @Test
    void maxUsesLessThanOne_rejectedAtDomainLevel() {
        // Command compact record constructor validates maxUses < 1
        assertThrows(IllegalArgumentException.class, () ->
                new CreateCouponUseCase.Command("VALID10", "PL", 0, null));
    }

    @Test
    void invalidCountryCode_rejectedAtDomainLevel() {
        // Country must be exactly 2 uppercase letters
        assertThrows(IllegalArgumentException.class, () ->
                new CreateCouponUseCase.Command("VALID10", "POL", 5, null));
    }

    @Test
    void lowercaseCountryCode_rejectedAtDomainLevel() {
        assertThrows(IllegalArgumentException.class, () ->
                new CreateCouponUseCase.Command("VALID10", "pl", 5, null));
    }

    @Test
    void duplicateCode_repositoryExceptionPropagates() {
        CreateCouponUseCase.Command command = new CreateCouponUseCase.Command("DUPE10", "PL", 5, null);
        when(couponRepository.save(any(Coupon.class)))
                .thenThrow(new RuntimeException("unique constraint violation"));

        assertThrows(RuntimeException.class, () -> service.create(command));
    }

    @Test
    void happyPath_couponSavedAndReturned() {
        CreateCouponUseCase.Command command = new CreateCouponUseCase.Command("SUMMER20", "DE", 100,
                LocalDateTime.now().plusDays(30));
        Coupon expected = savedCoupon("SUMMER20", "DE", 100);
        when(couponRepository.save(any(Coupon.class))).thenReturn(expected);

        Coupon result = service.create(command);

        assertNotNull(result);
        assertEquals("SUMMER20", result.getCode());
        assertEquals("DE", result.getCountry());
        assertEquals(100, result.getMaxUses());
        assertEquals(Coupon.Status.ACTIVE, result.getStatus());
    }

    @Test
    void happyPath_codeIsNormalisedToUppercase() {
        // Command normalises to uppercase internally
        CreateCouponUseCase.Command command = new CreateCouponUseCase.Command("save10", "PL", 1, null);
        assertEquals("SAVE10", command.code());
    }
}
