-- V3__create_indexes.sql
-- Creates performance-critical indexes for the coupon service.
--
-- Indexes deliberately omitted:
--   coupon(code)                     — already covered by the UNIQUE constraint,
--                                      which creates an implicit B-tree index.
--   coupon_usage(idempotency_key)    — already covered by the UNIQUE constraint.
--   coupon(status)                   — low selectivity (only 2 distinct values);
--                                      the query planner will prefer a sequential
--                                      scan and would ignore this index in practice.

-- Supports the most frequent query in the redeem flow: checking whether a given
-- user has already redeemed a specific coupon (coupon_usage.existsByUserIdAndCouponCode).
-- The composite index covers both predicates in a single index scan, avoiding a
-- table heap fetch for this equality lookup.
CREATE INDEX idx_coupon_usage_user_coupon
    ON coupon_usage (user_id, coupon_code);