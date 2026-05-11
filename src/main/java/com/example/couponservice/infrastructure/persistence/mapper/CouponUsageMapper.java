package com.example.couponservice.infrastructure.persistence.mapper;

import com.example.couponservice.domain.model.CouponUsage;
import com.example.couponservice.infrastructure.persistence.entity.CouponUsageEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between {@link CouponUsageEntity} and the domain {@link CouponUsage} record.
 *
 * <p>All field names match between entity and record, so no explicit {@code @Mapping} annotations
 * are required. Null-safe by default.
 */
@Mapper(componentModel = "spring")
public interface CouponUsageMapper {

    /**
     * Maps a JPA entity to the immutable domain record.
     *
     * @param entity JPA entity, may be {@code null}
     * @return domain record, or {@code null} if entity is {@code null}
     */
    CouponUsage toDomain(CouponUsageEntity entity);

    /**
     * Maps the domain record to a JPA entity suitable for persistence.
     *
     * @param usage domain record, may be {@code null}
     * @return JPA entity, or {@code null} if usage is {@code null}
     */
    CouponUsageEntity toEntity(CouponUsage usage);
}