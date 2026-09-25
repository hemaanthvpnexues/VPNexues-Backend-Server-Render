-- FIRST200: allow any first-order cart size (was min ₹500 — blocked carts like ₹410)
UPDATE coupons
SET min_order_value = 0
WHERE code = 'FIRST200';
