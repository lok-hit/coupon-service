-- V4__add_version_to_coupon.sql
-- Adds the optimistic-locking version column to the coupon table.
-- Required by CouponEntity's @Version field (Hibernate optimistic concurrency control).
-- Existing rows default to version = 0.

ALTER TABLE coupon
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
