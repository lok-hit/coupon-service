-- V1__create_coupon_table.sql
-- Creates the coupon table — the core entity of the coupon service.
-- Stores coupon definitions including redemption limits, geographic restrictions,
-- and lifecycle status.
-- The UNIQUE constraint on code is a database-level guarantee that prevents
-- duplicate codes even under concurrent inserts; the application also enforces
-- this, but the DB constraint is the authoritative safety net.

CREATE TABLE coupon (
    id            UUID         NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    country       VARCHAR(2)   NOT NULL,
    max_uses      INT          NOT NULL,
    current_uses  INT          NOT NULL DEFAULT 0,
    status        VARCHAR(10)  NOT NULL,
    valid_until   TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_coupon             PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code        UNIQUE (code),
    CONSTRAINT chk_coupon_status     CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_coupon_max_uses   CHECK (max_uses >= 1),
    CONSTRAINT chk_coupon_curr_uses  CHECK (current_uses >= 0)
);