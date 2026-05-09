package com.example.couponservice.domain.service;

import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.out.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCouponServiceTest {

    @Mock CouponRepository couponRepository;

    GetCouponService service;

    @BeforeEach
    void setUp() {
        service = new GetCouponService(couponRepository);
    }

    private Coupon coupon(String code) {
        return new Coupon(UUID.randomUUID(), code, "PL", 10, 0,
                Coupon.Status.ACTIVE, null, LocalDateTime.now());
    }

    @Test
    void getByCode_couponNotFound_throwsCouponNotFoundException() {
        when(couponRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        CouponNotFoundException ex = assertThrows(CouponNotFoundException.class,
                () -> service.getByCode("MISSING"));

        assertTrue(ex.getMessage().contains("MISSING"));
    }

    @Test
    void getByCode_happyPath_returnsCoupon() {
        Coupon expected = coupon("FIND10");
        when(couponRepository.findByCode("FIND10")).thenReturn(Optional.of(expected));

        Coupon result = service.getByCode("FIND10");

        assertSame(expected, result);
    }

    @Test
    void getByCode_normalisesCodeToUppercase() {
        Coupon expected = coupon("FIND10");
        when(couponRepository.findByCode("FIND10")).thenReturn(Optional.of(expected));

        // lowercase input should be normalised before lookup
        Coupon result = service.getByCode("find10");

        assertSame(expected, result);
    }

    @Test
    void getAll_happyPath_returnsPage() {
        Coupon c1 = coupon("CODE1");
        Coupon c2 = coupon("CODE2");
        Page<Coupon> expectedPage = new Page<>(List.of(c1, c2), 0, 10, 2L);
        Pageable pageable = new Pageable(0, 10);
        when(couponRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<Coupon> result = service.getAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.content().size());
        assertEquals(2L, result.totalElements());
        assertEquals(1, result.getTotalPages());
    }
}
