package com.example.couponservice.infrastructure.config;

import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.in.GetCouponUseCase;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import com.example.couponservice.domain.port.out.CouponRepository;
import com.example.couponservice.domain.port.out.CouponUsageRepository;
import com.example.couponservice.domain.port.out.GeoLocationPort;
import com.example.couponservice.domain.service.CreateCouponService;
import com.example.couponservice.domain.service.GetCouponService;
import com.example.couponservice.domain.service.RedeemCouponService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires domain services (pure-Java, no Spring annotations) as Spring beans,
 * injecting the infrastructure adapters that implement the domain output ports.
 */
@Configuration
public class DomainConfiguration {

    /** Creates the create-coupon use-case backed by the repository adapter. */
    @Bean
    public CreateCouponUseCase createCouponUseCase(CouponRepository couponRepository) {
        return new CreateCouponService(couponRepository);
    }

    /** Creates the get-coupon use-case backed by the repository adapter. */
    @Bean
    public GetCouponUseCase getCouponUseCase(CouponRepository couponRepository) {
        return new GetCouponService(couponRepository);
    }

    /** Creates the redeem-coupon use-case backed by all required adapters. */
    @Bean
    public RedeemCouponUseCase redeemCouponUseCase(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            GeoLocationPort geoLocationPort) {
        return new RedeemCouponService(couponRepository, couponUsageRepository, geoLocationPort);
    }
}