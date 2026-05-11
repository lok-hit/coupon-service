package com.example.couponservice.infrastructure.persistence;

import com.example.couponservice.domain.model.Coupon;
import com.example.couponservice.domain.model.page.Page;
import com.example.couponservice.domain.model.page.Pageable;
import com.example.couponservice.domain.port.out.CouponRepository;
import com.example.couponservice.infrastructure.persistence.mapper.CouponMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that bridges the domain {@link CouponRepository} output port to Spring Data JPA.
 *
 * <p>Thread-safety: this class is stateless — all state lives in the database. HikariCP
 * assigns a JDBC connection per request; Spring Data JPA proxies are thread-safe.
 * No synchronization is needed.
 */
@Repository
public class CouponRepositoryAdapter implements CouponRepository {

    private final JpaCouponRepository jpaRepository;
    private final CouponMapper mapper;

    public CouponRepositoryAdapter(JpaCouponRepository jpaRepository, CouponMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The code is normalized to uppercase before querying so that lookups are
     * case-insensitive at the application level (the DB stores codes in uppercase).
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Coupon> findByCode(String code) {
        return jpaRepository.findByCode(code.toUpperCase())
                .map(mapper::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Coupon save(Coupon coupon) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(coupon)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converts between the domain {@link Pageable} and Spring Data's pageable,
     * then wraps the result back into the domain {@link Page}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Coupon> findAll(Pageable pageable) {
        org.springframework.data.domain.Page<com.example.couponservice.infrastructure.persistence.entity.CouponEntity> springPage =
                jpaRepository.findAll(PageRequest.of(pageable.page(), pageable.size()));
        List<Coupon> coupons = springPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();
        return new Page<>(coupons, pageable.page(), pageable.size(), springPage.getTotalElements());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes an atomic CAS UPDATE — no external locking required. Returns 1 if
     * the row was incremented, 0 if {@code currentUses} already equals {@code maxUses}.
     */
    @Override
    @Transactional
    public int incrementUsage(String code) {
        return jpaRepository.incrementUsage(code);
    }
}