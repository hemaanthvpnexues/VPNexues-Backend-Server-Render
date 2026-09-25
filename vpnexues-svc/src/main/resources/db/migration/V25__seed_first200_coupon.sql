-- FIRST200 was never applied: V22__first_time_buyer_coupon.sql shares version 22
-- with an earlier V22__add_firebase_uid_to_users.sql, so Flyway skipped it.
-- Seed here as V25 (new version) + min_order 0 so first carts of any size work.
INSERT INTO coupons (code, type, value, active, min_order_value)
VALUES ('FIRST200', 'FLAT', 200.00, TRUE, 0.00)
ON CONFLICT (code) DO UPDATE
  SET value = EXCLUDED.value,
      active = TRUE,
      min_order_value = EXCLUDED.min_order_value;
