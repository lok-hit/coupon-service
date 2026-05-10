package com.example.couponservice.api.controller;

import com.example.couponservice.api.dto.request.CreateCouponRequest;
import com.example.couponservice.api.dto.request.RedeemCouponRequest;
import com.example.couponservice.api.dto.response.CouponResponse;
import com.example.couponservice.api.dto.response.CouponUsageResponse;
import com.example.couponservice.api.mapper.CouponApiMapper;
import com.example.couponservice.api.mapper.CouponUsageApiMapper;
import com.example.couponservice.api.util.IpExtractor;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.in.CreateCouponUseCase;
import com.example.couponservice.domain.port.in.GetCouponUseCase;
import com.example.couponservice.domain.port.in.RedeemCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.UUID;

/**
 * REST controller exposing coupon management and redemption endpoints.
 */
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons", description = "Coupon management and redemption")
public class CouponController {

    private final CreateCouponUseCase createCouponUseCase;
    private final GetCouponUseCase getCouponUseCase;
    private final RedeemCouponUseCase redeemCouponUseCase;
    private final CouponApiMapper couponApiMapper;
    private final CouponUsageApiMapper couponUsageApiMapper;
    private final IpExtractor ipExtractor;

    /** Constructs the controller with all required use cases and helpers. */
    public CouponController(CreateCouponUseCase createCouponUseCase,
                            GetCouponUseCase getCouponUseCase,
                            RedeemCouponUseCase redeemCouponUseCase,
                            CouponApiMapper couponApiMapper,
                            CouponUsageApiMapper couponUsageApiMapper,
                            IpExtractor ipExtractor) {
        this.createCouponUseCase = createCouponUseCase;
        this.getCouponUseCase = getCouponUseCase;
        this.redeemCouponUseCase = redeemCouponUseCase;
        this.couponApiMapper = couponApiMapper;
        this.couponUsageApiMapper = couponUsageApiMapper;
        this.ipExtractor = ipExtractor;
    }

    /**
     * Creates a new coupon.
     *
     * @param request the coupon creation payload
     * @return the created coupon
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon", description = "Creates a new coupon with the specified code, country restriction, and usage limit")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coupon created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public CouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        var command = new CreateCouponUseCase.Command(
                request.code(), request.country(), request.maxUses(), request.validUntil()
        );
        return couponApiMapper.toResponse(createCouponUseCase.create(command));
    }

    /**
     * Redeems a coupon for a specific user.
     *
     * @param code           the coupon code to redeem
     * @param request        the redemption payload containing the user identifier
     * @param idempotencyKey optional idempotency key; a UUID is generated if absent
     * @param httpRequest    the HTTP request used for client IP extraction
     * @return the coupon usage record
     */
    @PostMapping("/{code}/redeem")
    @Operation(summary = "Redeem a coupon", description = "Redeems a coupon for the specified user, enforcing geo-restriction and idempotency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon redeemed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Country not allowed for this coupon"),
            @ApiResponse(responseCode = "404", description = "Coupon not found"),
            @ApiResponse(responseCode = "409", description = "Coupon not active, exhausted, or already used by this user"),
            @ApiResponse(responseCode = "503", description = "Geo-location service unavailable")
    })
    public CouponUsageResponse redeem(
            @PathVariable String code,
            @RequestBody @Valid RedeemCouponRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest
    ) {
        String resolvedKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();
        String sourceIp = ipExtractor.extractClientIp(httpRequest);
        var command = new RedeemCouponUseCase.Command(code, request.userId(), sourceIp, resolvedKey);
        return couponUsageApiMapper.toResponse(redeemCouponUseCase.redeem(command));
    }

    /**
     * Retrieves a single coupon by its code.
     *
     * @param code the coupon code
     * @return the coupon details
     */
    @GetMapping("/{code}")
    @Operation(summary = "Get a coupon by code", description = "Returns coupon details for the given coupon code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon found"),
            @ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    public CouponResponse getByCode(@PathVariable String code) {
        return couponApiMapper.toResponse(getCouponUseCase.getByCode(code));
    }

    /**
     * Returns a paginated list of all coupons.
     *
     * @param page zero-based page index (default 0)
     * @param size number of items per page (default 20)
     * @return a page of coupon responses
     */
    @GetMapping
    @Operation(summary = "List all coupons", description = "Returns a paginated list of all coupons in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupons retrieved successfully")
    })
    public Page<CouponResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = new Pageable(page, size);
        return couponApiMapper.toResponse(getCouponUseCase.getAll(pageable));
    }
}
