package com.example.couponservice.api.mapper;

import com.example.couponservice.api.dto.response.CouponResponse;
import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper that converts domain {@link Coupon} objects to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface CouponApiMapper {

    /** Maps a single {@link Coupon} to a {@link CouponResponse}. */
    CouponResponse toResponse(Coupon coupon);

    /** Maps a page of {@link Coupon} objects to a page of {@link CouponResponse} objects. */
    default Page<CouponResponse> toResponse(Page<Coupon> page) {
        List<CouponResponse> content = page.content().stream()
                .map(this::toResponse)
                .toList();
        return new Page<>(content, page.pageNumber(), page.pageSize(), page.totalElements());
    }
}
