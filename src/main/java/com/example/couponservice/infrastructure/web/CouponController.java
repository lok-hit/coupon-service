package com.example.couponservice.infrastructure.web;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.in.GetCouponUseCase;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import com.example.couponservice.infrastructure.web.dto.request.CreateCouponRequest;
import com.example.couponservice.infrastructure.web.dto.request.RedeemCouponRequest;
import com.example.couponservice.infrastructure.web.dto.response.CouponResponse;
import com.example.couponservice.infrastructure.web.dto.response.CouponUsageResponse;
import com.example.couponservice.infrastructure.web.dto.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final GetCouponUseCase getCouponUseCase;
    private final RedeemCouponUseCase redeemCouponUseCase;

    public CouponController(CreateCouponUseCase createCouponUseCase,
                            GetCouponUseCase getCouponUseCase,
                            RedeemCouponUseCase redeemCouponUseCase) {
        this.createCouponUseCase = createCouponUseCase;
        this.getCouponUseCase = getCouponUseCase;
        this.redeemCouponUseCase = redeemCouponUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        var command = new CreateCouponUseCase.Command(
                request.code(), request.country(), request.maxUses(), request.validUntil()
        );
        return toResponse(createCouponUseCase.create(command));
    }

    @GetMapping("/{code}")
    public CouponResponse getByCode(@PathVariable String code) {
        return toResponse(getCouponUseCase.getByCode(code));
    }

    @GetMapping
    public PageResponse<CouponResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = new Pageable(page, size);
        var result = getCouponUseCase.getAll(pageable);
        return new PageResponse<>(
                result.content().stream().map(this::toResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.getTotalPages()
        );
    }

    @PostMapping("/{code}/redeem")
    public CouponUsageResponse redeem(
            @PathVariable String code,
            @RequestBody @Valid RedeemCouponRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        var sourceIp = extractClientIp(httpRequest);
        var command = new RedeemCouponUseCase.Command(code, request.userId(), sourceIp, idempotencyKey);
        return toUsageResponse(redeemCouponUseCase.redeem(command));
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getCountry(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getStatus().name(),
                coupon.getValidUntil(),
                coupon.getCreatedAt()
        );
    }

    private CouponUsageResponse toUsageResponse(CouponUsage usage) {
        return new CouponUsageResponse(
                usage.id(),
                usage.couponCode(),
                usage.userId(),
                usage.usedAt(),
                usage.sourceIp(),
                usage.idempotencyKey()
        );
    }
}
