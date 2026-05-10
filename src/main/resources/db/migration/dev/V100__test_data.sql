-- V100__test_data.sql
-- Inserts fixture coupons for local development and integration testing.
-- This file lives under db/migration/dev/ and is applied only when the dev
-- Flyway location is active (e.g. spring.flyway.locations includes
-- classpath:db/migration/dev). It must never run in production.
-- All codes are stored as uppercase. created_at defaults to NOW().

INSERT INTO coupon (id, code, country, max_uses, current_uses, status, valid_until, created_at)
VALUES (gen_random_uuid(), 'WIOSNA2025', 'PL', 100, 0, 'ACTIVE', NULL, NOW());

INSERT INTO coupon (id, code, country, max_uses, current_uses, status, valid_until, created_at)
VALUES (gen_random_uuid(), 'SUMMER10', 'DE', 50, 0, 'ACTIVE', NOW() + INTERVAL '1 year', NOW());

INSERT INTO coupon (id, code, country, max_uses, current_uses, status, valid_until, created_at)
VALUES (gen_random_uuid(), 'EXPIRED01', 'PL', 10, 0, 'ACTIVE', '2024-01-01 00:00:00', NOW());

INSERT INTO coupon (id, code, country, max_uses, current_uses, status, valid_until, created_at)
VALUES (gen_random_uuid(), 'DISABLED1', 'PL', 10, 0, 'DISABLED', NULL, NOW());

INSERT INTO coupon (id, code, country, max_uses, current_uses, status, valid_until, created_at)
VALUES (gen_random_uuid(), 'GLOBALVIP', 'US', 1000, 0, 'ACTIVE', NULL, NOW());