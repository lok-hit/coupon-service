package com.example.couponservice.domain.port.in;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;

public interface GetCouponUseCase {

    Coupon getByCode(String code);

    Page<Coupon> getAll(Pageable pageable);
}
