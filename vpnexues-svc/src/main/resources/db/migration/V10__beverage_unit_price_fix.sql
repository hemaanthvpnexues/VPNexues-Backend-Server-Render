-- Fixes 15 beverage products where bottle image (300ml/500ml/300g) mismatched DB unit/price showing pcs/200g
-- Based on user list 07-Sep: image content vs price display alignment

-- 1. Cold-Pressed Juice Mix: img 300ml, DB was 500ml 120 -> change to 300ml keep 120
UPDATE products SET unit = '300ml' WHERE slug = 'cold-pressed-juice-mix';

-- 2. Herbal Green Tea: img 300ml, DB 50 bags 90 -> 300ml 90 (price already 90 in IN override)
UPDATE products SET unit = '300ml' WHERE slug = 'herbal-green-tea';

-- 3. Jasmine Green Tea: same as herbal -> 300ml 90 (was 50 bags 425 -> fix price to 90)
UPDATE products SET unit = '300ml' WHERE slug = 'jasmine-green-tea';
UPDATE product_price_overrides SET price = 90.00, old_price = 110.00 WHERE product_id = (SELECT id FROM products WHERE slug = 'jasmine-green-tea') AND country_code = 'IN';
-- Keep SGD base 6.90 for SG/US/AE (premium), IN override now 90 matches Herbal

-- 4. Iced Coffee: img 500ml 365, DB 400ml 365 -> 500ml
UPDATE products SET unit = '500ml' WHERE slug = 'iced-coffee';

-- 5. Kokum Sherbet: same as iced coffee -> 500ml keep 65
UPDATE products SET unit = '500ml' WHERE slug = 'kokum-sherbet';

-- 6. Mango Lassi: same -> 500ml keep 60
UPDATE products SET unit = '500ml' WHERE slug = 'mango-lassi';

-- 7. Masala Chai: img 500ml 340, DB 200 bags 340 -> 500ml keep 340
UPDATE products SET unit = '500ml' WHERE slug = 'masala-chai';

-- 8. Moringa Tea: img 500ml 80, DB 30 bags 80 -> 500ml keep 80
UPDATE products SET unit = '500ml' WHERE slug = 'moringa-tea';

-- 9. Rose Milk: img 500ml 45, DB 300ml 45 -> 500ml
UPDATE products SET unit = '500ml' WHERE slug = 'rose-milk';

-- 10. Protein Shake: same as rose milk -> 500ml keep 465 (unit only, price per user: keep current 465)
UPDATE products SET unit = '500ml' WHERE slug = 'protein-shake';

-- 11. Nannari Sarbath: same -> 500ml keep 50
UPDATE products SET unit = '500ml' WHERE slug = 'nannari-sarbath';

-- 12. Tamarind Drink: same -> 500ml keep 45
UPDATE products SET unit = '500ml' WHERE slug = 'tamarind-drink';

-- 13. Tender Coconut Water: img 500ml 30, DB pc 30 -> 500ml (screenshot shows · pc should be · 500ml)
UPDATE products SET unit = '500ml' WHERE slug = 'tender-coconut-water';

-- 14. Turmeric Latte Mix: img 300g 120, DB 200g 120 -> 300g
UPDATE products SET unit = '300g' WHERE slug = 'turmeric-latte-mix';

-- 15. Vetiver Cooler: img 500ml 55, DB 400ml 55 -> 500ml
UPDATE products SET unit = '500ml' WHERE slug = 'vetiver-cooler';
