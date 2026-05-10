package com.example.couponservice.api.controller;

import com.example.couponservice.api.dto.request.CreateCouponRequest;
import com.example.couponservice.api.dto.request.RedeemCouponRequest;
import com.example.couponservice.api.dto.response.CouponResponse;
import com.example.couponservice.api.dto.response.CouponUsageResponse;
import com.example.couponservice.api.exception.GlobalExceptionHandler;
import com.example.couponservice.api.mapper.CouponApiMapper;
import com.example.couponservice.api.mapper.CouponUsageApiMapper;
import com.example.couponservice.api.util.IpExtractor;
import com.example.couponservice.domain.exception.CountryNotAllowedException;
import com.example.couponservice.domain.exception.CouponExhaustedException;
import com.example.couponservice.domain.exception.CouponNotFoundException;
import com.example.couponservice.domain.exception.GeoLocationUnavailableException;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.in.GetCouponUseCase;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@Import(GlobalExceptionHandler.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateCouponUseCase createCouponUseCase;

    @MockitoBean
    private GetCouponUseCase getCouponUseCase;

    @MockitoBean
    private RedeemCouponUseCase redeemCouponUseCase;

    @MockitoBean
    private CouponApiMapper couponApiMapper;

    @MockitoBean
    private CouponUsageApiMapper couponUsageApiMapper;

    @MockitoBean
    private IpExtractor ipExtractor;

    private static final UUID COUPON_ID = UUID.randomUUID();
    private static final UUID USAGE_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.now();

    private CouponResponse aCouponResponse() {
        return new CouponResponse(COUPON_ID, "TEST123", "PL", 100, 0, "ACTIVE", null, NOW);
    }

    private CouponUsageResponse aUsageResponse() {
        return new CouponUsageResponse(USAGE_ID, "TEST123", "user-1", NOW);
    }

    private Coupon aCoupon() {
        return new Coupon(COUPON_ID, "TEST123", "PL", 100, 0, Coupon.Status.ACTIVE, null, NOW);
    }

    private CouponUsage aCouponUsage() {
        return new CouponUsage(USAGE_ID, "TEST123", "user-1", NOW, "127.0.0.1", UUID.randomUUID().toString());
    }

    @Test
    void createCoupon_happyPath_returns201() throws Exception {
        var request = new CreateCouponRequest("TEST123", "PL", 100, null);
        when(createCouponUseCase.create(any())).thenReturn(aCoupon());
        when(couponApiMapper.toResponse(any(Coupon.class))).thenReturn(aCouponResponse());

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEST123"))
                .andExpect(jsonPath("$.country").value("PL"));
    }

    @Test
    void createCoupon_blankCode_returns400() throws Exception {
        var request = new CreateCouponRequest("", "PL", 100, null);

        mockMvc.perform(post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void redeemCoupon_happyPath_returns200() throws Exception {
        var request = new RedeemCouponRequest("user-1");
        when(ipExtractor.extractClientIp(any())).thenReturn("127.0.0.1");
        when(redeemCouponUseCase.redeem(any())).thenReturn(aCouponUsage());
        when(couponUsageApiMapper.toResponse(any(CouponUsage.class))).thenReturn(aUsageResponse());

        mockMvc.perform(post("/api/v1/coupons/TEST123/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("TEST123"))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    void redeemCoupon_couponNotFound_returns404() throws Exception {
        var request = new RedeemCouponRequest("user-1");
        when(ipExtractor.extractClientIp(any())).thenReturn("127.0.0.1");
        when(redeemCouponUseCase.redeem(any())).thenThrow(new CouponNotFoundException("MISSING"));

        mockMvc.perform(post("/api/v1/coupons/MISSING/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void redeemCoupon_couponExhausted_returns409() throws Exception {
        var request = new RedeemCouponRequest("user-1");
        when(ipExtractor.extractClientIp(any())).thenReturn("127.0.0.1");
        when(redeemCouponUseCase.redeem(any())).thenThrow(new CouponExhaustedException("TEST123"));

        mockMvc.perform(post("/api/v1/coupons/TEST123/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_EXHAUSTED"));
    }

    @Test
    void redeemCoupon_countryNotAllowed_returns403() throws Exception {
        var request = new RedeemCouponRequest("user-1");
        when(ipExtractor.extractClientIp(any())).thenReturn("127.0.0.1");
        when(redeemCouponUseCase.redeem(any())).thenThrow(new CountryNotAllowedException("DE", "PL"));

        mockMvc.perform(post("/api/v1/coupons/TEST123/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));
    }

    @Test
    void redeemCoupon_geoLocationUnavailable_returns503() throws Exception {
        var request = new RedeemCouponRequest("user-1");
        when(ipExtractor.extractClientIp(any())).thenReturn("5.5.5.5");
        when(redeemCouponUseCase.redeem(any())).thenThrow(new GeoLocationUnavailableException("5.5.5.5"));

        mockMvc.perform(post("/api/v1/coupons/TEST123/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GEO_LOCATION_UNAVAILABLE"));
    }

    @Test
    void getCouponByCode_happyPath_returns200() throws Exception {
        when(getCouponUseCase.getByCode("TEST123")).thenReturn(aCoupon());
        when(couponApiMapper.toResponse(any(Coupon.class))).thenReturn(aCouponResponse());

        mockMvc.perform(get("/api/v1/coupons/TEST123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST123"));
    }

    @Test
    void getCouponByCode_notFound_returns404() throws Exception {
        when(getCouponUseCase.getByCode("MISSING")).thenThrow(new CouponNotFoundException("MISSING"));

        mockMvc.perform(get("/api/v1/coupons/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void getAllCoupons_happyPathWithPagination_returns200() throws Exception {
        var domainPage = new Page<>(List.of(aCoupon()), 0, 20, 1L);
        var responsePage = new Page<>(List.of(aCouponResponse()), 0, 20, 1L);
        when(getCouponUseCase.getAll(any(Pageable.class))).thenReturn(domainPage);
        when(couponApiMapper.toResponse(any(Page.class))).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/coupons").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
