-- FIRST200: first-order offer only on carts of ₹500 or more
-- Below ₹500 → "You are not eligible for this coupon code"
UPDATE coupons
SET min_order_value = 500.00
WHERE code = 'FIRST200';
