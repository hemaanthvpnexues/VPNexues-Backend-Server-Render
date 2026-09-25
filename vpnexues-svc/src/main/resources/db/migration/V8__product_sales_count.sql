-- Bestseller ranking: products with higher sales_count appear first in Shop / Small Box / Big Box
-- Initial seed: Tomato first, Onion second, then daily staples — matches Head's request.
-- Future: OrderService increments sales_count on every checkout so ranking becomes realtime.

ALTER TABLE products ADD COLUMN sales_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_products_sales_count ON products (sales_count DESC, name ASC);

-- Top sellers (India staples) — 1000..600
UPDATE products SET sales_count = 1000 WHERE slug = 'vine-tomatoes';
UPDATE products SET sales_count =  950 WHERE slug = 'red-onions';
UPDATE products SET sales_count =  900 WHERE slug = 'cauliflower';
UPDATE products SET sales_count =  850 WHERE slug = 'broccoli-head';
UPDATE products SET sales_count =  840 WHERE slug = 'white-radish';
UPDATE products SET sales_count =  830 WHERE slug = 'organic-carrots';
UPDATE products SET sales_count =  820 WHERE slug = 'cucumber';
UPDATE products SET sales_count =  810 WHERE slug = 'green-beans';
UPDATE products SET sales_count =  800 WHERE slug = 'beetroot';
UPDATE products SET sales_count =  790 WHERE slug = 'french-beans';
UPDATE products SET sales_count =  780 WHERE slug = 'bell-peppers-mix';
UPDATE products SET sales_count =  770 WHERE slug = 'baby-spinach';
UPDATE products SET sales_count =  760 WHERE slug = 'mint-leaves';
UPDATE products SET sales_count =  750 WHERE slug = 'green-capsicum';
UPDATE products SET sales_count =  740 WHERE slug = 'okra-ladyfinger';
UPDATE products SET sales_count =  730 WHERE slug = 'bitter-gourd';
UPDATE products SET sales_count =  720 WHERE slug = 'bottle-gourd';
UPDATE products SET sales_count =  710 WHERE slug = 'eggplant';
UPDATE products SET sales_count =  700 WHERE slug = 'sweet-potato';
UPDATE products SET sales_count =  690 WHERE slug = 'purple-cabbage';
UPDATE products SET sales_count =  680 WHERE slug = 'pumpkin';
UPDATE products SET sales_count =  670 WHERE slug = 'ridge-gourd';
UPDATE products SET sales_count =  660 WHERE slug = 'cherry-tomatoes';
UPDATE products SET sales_count =  650 WHERE slug = 'spring-onions';
UPDATE products SET sales_count =  640 WHERE slug = 'curry-leaves';
UPDATE products SET sales_count =  630 WHERE slug = 'colocasia-leaves';
UPDATE products SET sales_count =  620 WHERE slug = 'fresh-coconut';
UPDATE products SET sales_count =  610 WHERE slug = 'coconut-fresh';
UPDATE products SET sales_count =  600 WHERE slug = 'coconut-milk';

-- Next tier: fruits & daily use — 590..450
UPDATE products SET sales_count =  590 WHERE slug = 'fresh-bananas';
UPDATE products SET sales_count =  580 WHERE slug = 'alphonso-mango';
UPDATE products SET sales_count =  570 WHERE slug = 'fresh-papaya';
UPDATE products SET sales_count =  560 WHERE slug = 'watermelon';
UPDATE products SET sales_count =  550 WHERE slug = 'fresh-oranges';
UPDATE products SET sales_count =  540 WHERE slug = 'kiwi-fruit';
UPDATE products SET sales_count =  530 WHERE slug = 'fresh-pomegranate';
UPDATE products SET sales_count =  520 WHERE slug = 'green-apples';
UPDATE products SET sales_count =  510 WHERE slug = 'red-grapes';
UPDATE products SET sales_count =  500 WHERE slug = 'basmati-rice';
UPDATE products SET sales_count =  490 WHERE slug = 'brown-rice';
UPDATE products SET sales_count =  480 WHERE slug = 'farm-fresh-cow-ghee';
UPDATE products SET sales_count =  470 WHERE slug = 'cold-pressed-coconut-oil';
UPDATE products SET sales_count =  460 WHERE slug = 'groundnut-oil';
UPDATE products SET sales_count =  450 WHERE slug = 'free-range-eggs';

-- Legacy/fallback slugs if any (ensure tomato/onion legacy also ranked so they don't float to top accidentally)
UPDATE products SET sales_count =  950 WHERE slug = 'red-onions-legacy' AND sales_count = 0;
UPDATE products SET sales_count =  999 WHERE slug = 'farm-tomatoes-legacy' AND sales_count = 0;
