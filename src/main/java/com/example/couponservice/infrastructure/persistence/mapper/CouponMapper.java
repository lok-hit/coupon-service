package com.example.couponservice.infrastructure.persistence.mapper;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.infrastructure.persistence.entity.CouponEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between {@link CouponEntity} and the domain {@link Coupon}.
 *
 * <p>Null-safe by default: MapStruct generates null checks; passing {@code null}
 * returns {@code null} for reference-type targets.
 */
@Mapper(componentModel = "spring")
public interface CouponMapper {

    /**
     * Maps a JPA entity to the immutable domain object.
     * The {@code version} field in the entity has no counterpart in the domain model
     * and is silently ignored.
     *
     * @param entity JPA entity, may be {@code null}
     * @return domain object, or {@code null} if entity is {@code null}
     */
    Coupon toDomain(CouponEntity entity);

    /**
     * Maps the domain object to a JPA entity suitable for persistence.
     * The {@code version} field is managed by JPA and must not be set manually.
     *
     * @param coupon domain object, may be {@code null}
     * @return JPA entity, or {@code null} if coupon is {@code null}
     */
    @Mapping(target = "version", ignore = true)
    CouponEntity toEntity(Coupon coupon);
}