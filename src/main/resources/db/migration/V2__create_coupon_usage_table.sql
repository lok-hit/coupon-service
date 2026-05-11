-- V2__create_coupon_usage_table.sql
-- Creates the coupon_usage table, recording each successful redemption event.
-- The foreign key from coupon_code to coupon(code) is intentionally omitted:
-- under high write volume, an FK constraint causes row-level share locks on the
-- parent (coupon) row for every insert, creating lock contention when many users
-- redeem concurrently. Referential integrity is enforced at the application level
-- instead (RedeemCouponService verifies the coupon exists before inserting usage).

CREATE TABLE coupon_usage (
    id               UUID         NOT NULL,
    coupon_code      VARCHAR(50)  NOT NULL,
    user_id          VARCHAR(255) NOT NULL,
    used_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    source_ip        VARCHAR(45)  NOT NULL,
    idempotency_key  VARCHAR(36)  NOT NULL,

    CONSTRAINT pk_coupon_usage                 PRIMARY KEY (id),
    CONSTRAINT uq_coupon_usage_idempotency_key UNIQUE (idempotency_key)
);