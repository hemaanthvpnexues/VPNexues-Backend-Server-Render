-- First-time buyer coupon: ₹200 flat discount for new customers
INSERT INTO coupons (code, type, value, active, min_order_value)
VALUES ('FIRST200', 'FLAT', 200.00, TRUE, 500.00)
ON CONFLICT (code) DO UPDATE
  SET value = EXCLUDED.value,
      active = TRUE,
      min_order_value = EXCLUDED.min_order_value;
