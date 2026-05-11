package com.example.couponservice.api.mapper;

import com.example.couponservice.api.dto.response.CouponUsageResponse;
import com.example.couponservice.domain.model.CouponUsage;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper that converts domain {@link CouponUsage} objects to API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface CouponUsageApiMapper {

    /** Maps a {@link CouponUsage} to a {@link CouponUsageResponse}. */
    CouponUsageResponse toResponse(CouponUsage usage);
}
