-- Repeatable migration: sample orders, users, addresses, payments for dashboard.
-- Idempotent — uses DO blocks to skip if data already exists.

-- ============================================================
-- 1. CUSTOMERS (10 across 4 countries)
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM users WHERE phone = '+919876543210') THEN
    INSERT INTO users (id, phone, name, email, created_at, updated_at) VALUES
      ('a1000000-0000-0000-0000-000000000001', '+919876543210', 'Arjun Mehta',    'arjun.mehta@email.com',    now() - interval '21 days', now() - interval '21 days'),
      ('a1000000-0000-0000-0000-000000000002', '+919876543211', 'Priya Sharma',   'priya.sharma@email.com',   now() - interval '20 days', now() - interval '20 days'),
      ('a1000000-0000-0000-0000-000000000003', '+919876543212', 'Rahul Verma',    'rahul.verma@email.com',    now() - interval '19 days', now() - interval '19 days'),
      ('a1000000-0000-0000-0000-000000000004', '+6591234567',   'Wei Lin Tan',    'weilin.tan@email.com',     now() - interval '22 days', now() - interval '22 days'),
      ('a1000000-0000-0000-0000-000000000005', '+6591234568',   'Sarah Ng',       'sarah.ng@email.com',       now() - interval '18 days', now() - interval '18 days'),
      ('a1000000-0000-0000-0000-000000000006', '+6591234569',   'David Lee',      'david.lee@email.com',      now() - interval '17 days', now() - interval '17 days'),
      ('a1000000-0000-0000-0000-000000000007', '+12125551001',  'Mike Johnson',   'mike.j@email.com',         now() - interval '20 days', now() - interval '20 days'),
      ('a1000000-0000-0000-0000-000000000008', '+12125551002',  'Emily Chen',     'emily.chen@email.com',     now() - interval '16 days', now() - interval '16 days'),
      ('a1000000-0000-0000-0000-000000000009', '+971501234567', 'Ahmed Al-Rashid','ahmed.rashid@email.com',   now() - interval '19 days', now() - interval '19 days'),
      ('a1000000-0000-0000-0000-000000000010', '+971501234568', 'Fatima Hassan',  'fatima.hassan@email.com',  now() - interval '15 days', now() - interval '15 days');
  END IF;
END $$;

-- ============================================================
-- 2. ADDRESSES (1 per customer)
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM user_addresses WHERE user_id = 'a1000000-0000-0000-0000-000000000001') THEN
    INSERT INTO user_addresses (id, user_id, label, name, phone, address_line, city, pincode, state, is_default, created_at, updated_at) VALUES
      ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'Home', 'Arjun Mehta',    '+919876543210', '12 Marine Drive',       'Mumbai',    '400001', 'Maharashtra', true, now() - interval '21 days', now() - interval '21 days'),
      ('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002', 'Home', 'Priya Sharma',   '+919876543211', '45 MG Road',           'Bangalore', '560001', 'Karnataka',   true, now() - interval '20 days', now() - interval '20 days'),
      ('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000003', 'Home', 'Rahul Verma',    '+919876543212', '78 Connaught Place',    'New Delhi', '110001', 'Delhi',        true, now() - interval '19 days', now() - interval '19 days'),
      ('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000004', 'Home', 'Wei Lin Tan',    '+6591234567',   '10 Orchard Road',       'Singapore', '238841', 'Singapore',    true, now() - interval '22 days', now() - interval '22 days'),
      ('b1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000005', 'Home', 'Sarah Ng',       '+6591234568',   '22 Jurong East',        'Singapore', '609466', 'Singapore',    true, now() - interval '18 days', now() - interval '18 days'),
      ('b1000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000006', 'Home', 'David Lee',      '+6591234569',   '5 Bukit Timah Road',    'Singapore', '268828', 'Singapore',    true, now() - interval '17 days', now() - interval '17 days'),
      ('b1000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000007', 'Home', 'Mike Johnson',   '+12125551001',  '350 Fifth Avenue',      'New York',  '10118',  'New York',     true, now() - interval '20 days', now() - interval '20 days'),
      ('b1000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000008', 'Home', 'Emily Chen',     '+12125551002',  '100 Market Street',     'San Francisco', '94105', 'California',  true, now() - interval '16 days', now() - interval '16 days'),
      ('b1000000-0000-0000-0000-000000000009', 'a1000000-0000-0000-0000-000000000009', 'Home', 'Ahmed Al-Rashid','+971501234567', 'Sheikh Zayed Road',     'Dubai',     '00000',  'Dubai',        true, now() - interval '19 days', now() - interval '19 days'),
      ('b1000000-0000-0000-0000-000000000010', 'a1000000-0000-0000-0000-000000000010', 'Home', 'Fatima Hassan',  '+971501234568', '14 King Fahd Road',     'Riyadh',    '11564',  'Riyadh',       true, now() - interval '15 days', now() - interval '15 days');
  END IF;
END $$;

-- ============================================================
-- 3. ORDERS, ORDER ITEMS, PAYMENTS
--    40 orders: 10 per country, spread across 14 days,
--    mixed statuses + channels + payment methods.
-- ============================================================
DO $$
DECLARE
  v_exists boolean;
  v_p1 uuid; v_p2 uuid; v_p3 uuid; v_p4 uuid; v_p5 uuid;
  v_p6 uuid; v_p7 uuid; v_p8 uuid; v_p9 uuid; v_p10 uuid;
  v_p11 uuid; v_p12 uuid; v_p13 uuid; v_p14 uuid; v_p15 uuid;
  v_p16 uuid; v_p17 uuid; v_p18 uuid; v_p19 uuid; v_p20 uuid;
BEGIN
  SELECT EXISTS (SELECT 1 FROM orders LIMIT 1) INTO v_exists;
  IF v_exists THEN RETURN; END IF;

  -- Grab 20 product IDs
  SELECT id INTO v_p1  FROM products WHERE slug = 'organic-carrots';
  SELECT id INTO v_p2  FROM products WHERE slug = 'vine-tomatoes';
  SELECT id INTO v_p3  FROM products WHERE slug = 'baby-spinach';
  SELECT id INTO v_p4  FROM products WHERE slug = 'bell-peppers-mix';
  SELECT id INTO v_p5  FROM products WHERE slug = 'fresh-bananas';
  SELECT id INTO v_p6  FROM products WHERE slug = 'alphonso-mango';
  SELECT id INTO v_p7  FROM products WHERE slug = 'coconut-water';
  SELECT id INTO v_p8  FROM products WHERE slug = 'basmati-rice';
  SELECT id INTO v_p9  FROM products WHERE slug = 'honey-raw-wildflower';
  SELECT id INTO v_p10 FROM products WHERE slug = 'cold-pressed-coconut-oil';
  SELECT id INTO v_p11 FROM products WHERE slug = 'cherry-tomatoes';
  SELECT id INTO v_p12 FROM products WHERE slug = 'broccoli-head';
  SELECT id INTO v_p13 FROM products WHERE slug = 'green-apples';
  SELECT id INTO v_p14 FROM products WHERE slug = 'pomegranate';
  SELECT id INTO v_p15 FROM products WHERE slug = 'free-range-eggs';
  SELECT id INTO v_p16 FROM products WHERE slug = 'strawberries';
  SELECT id INTO v_p17 FROM products WHERE slug = 'herbal-green-tea';
  SELECT id INTO v_p18 FROM products WHERE slug = 'almonds';
  SELECT id INTO v_p19 FROM products WHERE slug = 'dragon-fruit-white';
  SELECT id INTO v_p20 FROM products WHERE slug = 'masala-chai';

  -- =========== INDIA (10 orders) ===========

  INSERT INTO orders (id, order_number, user_id, address_id, status, channel, item_total, discount, delivery_fee, tax, tip, grand_total, payment_method, country_code, created_at, updated_at)
  VALUES
    ('c1000000-0000-0000-0000-000000000001', 'VPN-260001', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'DELIVERED', 'SHOP',      12.50, 0, 2.00, 1.13, 0, 15.63, 'Card',      'IN', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000002', 'VPN-260002', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'DELIVERED', 'SMALL_BOX', 22.40, 0, 0.00, 2.02, 1.00, 25.42, 'UPI',        'IN', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000003', 'VPN-260003', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003', 'DELIVERED', 'SHOP',      35.70, 3.57, 2.00, 3.07, 0, 37.20, 'COD',        'IN', now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000004', 'VPN-260004', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'PROCESSING', 'SHOP',     18.30, 0, 2.00, 1.65, 0, 21.95, 'Card',       'IN', now() - interval '8 days',  now() - interval '8 days'),
    ('c1000000-0000-0000-0000-000000000005', 'VPN-260005', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'SHIPPED',   'BIG_BOX',   42.00, 4.20, 0.00, 3.40, 0, 41.20, 'UPI',        'IN', now() - interval '6 days',  now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000006', 'VPN-260006', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003', 'PLACED',    'SHOP',      9.80,  0, 2.00, 0.88, 0, 12.68, 'COD',        'IN', now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000007', 'VPN-260007', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'CANCELLED', 'SHOP',      28.90, 0, 2.00, 2.60, 0, 33.50, 'Card',       'IN', now() - interval '5 days',  now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000008', 'VPN-260008', 'a1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000002', 'DELIVERED', 'SHOP',      15.60, 0, 2.00, 1.40, 0, 19.00, 'UPI',        'IN', now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000009', 'VPN-260009', 'a1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003', 'CONFIRMED', 'SMALL_BOX', 19.50, 1.95, 0.00, 1.58, 0, 19.13, 'Card',       'IN', now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000010', 'VPN-260010', 'a1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 'PLACED',    'SHOP',      11.20, 0, 2.00, 1.01, 0, 14.21, 'UPI',        'IN', now() - interval '1 day',   now() - interval '1 day');

  -- =========== SINGAPORE (10 orders) ===========

  INSERT INTO orders (id, order_number, user_id, address_id, status, channel, item_total, discount, delivery_fee, tax, tip, grand_total, payment_method, country_code, created_at, updated_at)
  VALUES
    ('c1000000-0000-0000-0000-000000000011', 'VPN-260011', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004', 'DELIVERED', 'SHOP',      45.00, 0, 5.00, 3.15, 0, 53.15, 'Card',       'SG', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000012', 'VPN-260012', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', 'DELIVERED', 'SMALL_BOX', 32.50, 3.25, 0.00, 2.05, 0, 31.30, 'PayNow',    'SG', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000013', 'VPN-260013', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006', 'DELIVERED', 'SHOP',      58.00, 5.80, 5.00, 3.69, 0, 60.89, 'GrabPay',   'SG', now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000014', 'VPN-260014', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004', 'PROCESSING', 'BIG_BOX',  89.00, 0, 0.00, 6.23, 0, 95.23, 'Card',       'SG', now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000015', 'VPN-260015', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', 'SHIPPED',   'SHOP',      27.00, 0, 5.00, 2.24, 0, 34.24, 'PayNow',    'SG', now() - interval '5 days',  now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000016', 'VPN-260016', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006', 'PLACED',    'SHOP',      18.50, 0, 5.00, 1.65, 0, 25.15, 'GrabPay',   'SG', now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000017', 'VPN-260017', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004', 'CANCELLED', 'SHOP',      42.00, 4.20, 5.00, 3.01, 0, 45.81, 'Card',       'SG', now() - interval '4 days',  now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000018', 'VPN-260018', 'a1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', 'DELIVERED', 'SMALL_BOX', 24.80, 0, 0.00, 1.74, 0, 26.54, 'PayNow',    'SG', now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000019', 'VPN-260019', 'a1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006', 'PROCESSING', 'BIG_BOX',  67.50, 6.75, 0.00, 4.25, 0, 65.00, 'GrabPay',   'SG', now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000020', 'VPN-260020', 'a1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004', 'CONFIRMED', 'SHOP',      15.00, 0, 5.00, 1.40, 0, 21.40, 'Card',       'SG', now() - interval '1 day',   now() - interval '1 day');

  -- =========== USA (10 orders) ===========

  INSERT INTO orders (id, order_number, user_id, address_id, status, channel, item_total, discount, delivery_fee, tax, tip, grand_total, payment_method, country_code, created_at, updated_at)
  VALUES
    ('c1000000-0000-0000-0000-000000000021', 'VPN-260021', 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'DELIVERED', 'SHOP',      38.50, 0, 8.00, 3.08, 5.00, 54.58, 'Card',       'US', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000022', 'VPN-260022', 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', 'DELIVERED', 'SMALL_BOX', 52.00, 5.20, 0.00, 3.74, 0, 50.54, 'Apple Pay', 'US', now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000023', 'VPN-260023', 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'DELIVERED', 'SHOP',      29.90, 0, 8.00, 2.39, 3.00, 43.29, 'Card',       'US', now() - interval '9 days',  now() - interval '9 days'),
    ('c1000000-0000-0000-0000-000000000024', 'VPN-260024', 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', 'PROCESSING', 'BIG_BOX',  75.00, 7.50, 0.00, 5.01, 0, 72.51, 'Apple Pay', 'US', now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000025', 'VPN-260025', 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'SHIPPED',   'SHOP',      41.00, 0, 8.00, 3.28, 5.00, 57.28, 'Card',       'US', now() - interval '5 days',  now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000026', 'VPN-260026', 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', 'PLACED',    'SHOP',      22.50, 0, 8.00, 1.80, 0, 32.30, 'Apple Pay', 'US', now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000027', 'VPN-260027', 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'CANCELLED', 'SMALL_BOX', 34.00, 3.40, 0.00, 2.37, 0, 32.97, 'Card',       'US', now() - interval '4 days',  now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000028', 'VPN-260028', 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', 'DELIVERED', 'SHOP',      47.80, 0, 8.00, 3.82, 5.00, 64.62, 'Apple Pay', 'US', now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000029', 'VPN-260029', 'a1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007', 'CONFIRMED', 'BIG_BOX',  62.00, 6.20, 0.00, 4.14, 0, 59.94, 'Card',       'US', now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000030', 'VPN-260030', 'a1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000008', 'PLACED',    'SHOP',      18.00, 0, 8.00, 1.44, 0, 27.44, 'Apple Pay', 'US', now() - interval '1 day',   now() - interval '1 day');

  -- =========== UAE (10 orders) ===========

  INSERT INTO orders (id, order_number, user_id, address_id, status, channel, item_total, discount, delivery_fee, tax, tip, grand_total, payment_method, country_code, created_at, updated_at)
  VALUES
    ('c1000000-0000-0000-0000-000000000031', 'VPN-260031', 'a1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'DELIVERED', 'SHOP',      55.00, 0, 10.00, 2.75, 0, 67.75, 'Card',       'AE', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000032', 'VPN-260032', 'a1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', 'DELIVERED', 'SMALL_BOX', 38.00, 3.80, 0.00, 1.71, 0, 35.91, 'Card',       'AE', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000033', 'VPN-260033', 'a1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'DELIVERED', 'SHOP',      72.00, 7.20, 10.00, 3.24, 0, 78.04, 'Card',       'AE', now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000034', 'VPN-260034', 'a1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', 'PROCESSING', 'BIG_BOX',  95.00, 0, 0.00, 4.75, 0, 99.75, 'Card',       'AE', now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000035', 'VPN-260035', 'a1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'SHIPPED',   'SHOP',      48.50, 0, 10.00, 2.43, 0, 60.93, 'Card',       'AE', now() - interval '6 days',  now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000036', 'VPN-260036', 'a1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', 'PLACED',    'SHOP',      25.00, 0, 10.00, 1.25, 0, 36.25, 'Card',       'AE', now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000037', 'VPN-260037', 'a1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'CANCELLED', 'SMALL_BOX', 33.00, 3.30, 0.00, 1.49, 0, 31.19, 'Card',       'AE', now() - interval '4 days',  now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000038', 'VPN-260038', 'a1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', 'DELIVERED', 'SHOP',      42.00, 0, 10.00, 2.10, 0, 54.10, 'Card',       'AE', now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000039', 'VPN-260039', 'a1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'CONFIRMED', 'BIG_BOX',  80.00, 8.00, 0.00, 3.60, 0, 75.60, 'Card',       'AE', now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000040', 'VPN-260040', 'a1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000010', 'PLACED',    'SHOP',      20.00, 0, 10.00, 1.00, 0, 31.00, 'Card',       'AE', now() - interval '1 day',   now() - interval '1 day');

  -- ============================================================
  -- 4. ORDER ITEMS (2-4 items per order)
  -- ============================================================

  -- Order 1 (IN, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000001', v_p1,  'Organic Carrots',    2, 2.80, 5.60, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000001', v_p2,  'Vine Tomatoes',      1, 3.90, 3.90, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000001', v_p5,  'Fresh Bananas',      1, 3.20, 3.20, now() - interval '13 days', now() - interval '13 days');

  -- Order 2 (IN, DELIVERED, UPI)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000002', v_p6,  'Alphonso Mango',     2, 12.00, 24.00, now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000002', v_p3,  'Baby Spinach',       1, 3.20, 3.20, now() - interval '12 days', now() - interval '12 days');

  -- Order 3 (IN, DELIVERED, COD)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000003', v_p8,  'Basmati Rice',       2, 9.80, 19.60, now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000003', v_p10, 'Cold-Pressed Coconut Oil', 1, 14.50, 14.50, now() - interval '11 days', now() - interval '11 days');

  -- Order 4 (IN, PROCESSING, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000004', v_p4,  'Bell Peppers Mix',   2, 4.50, 9.00, now() - interval '8 days', now() - interval '8 days'),
    ('c1000000-0000-0000-0000-000000000004', v_p11, 'Cherry Tomatoes',    1, 4.20, 4.20, now() - interval '8 days', now() - interval '8 days'),
    ('c1000000-0000-0000-0000-000000000004', v_p7,  'Coconut Water',      2, 4.50, 9.00, now() - interval '8 days', now() - interval '8 days');

  -- Order 5 (IN, SHIPPED, UPI)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000005', v_p9,  'Honey (Raw Wildflower)', 2, 14.90, 29.80, now() - interval '6 days', now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000005', v_p12, 'Broccoli Head',      3, 3.50, 10.50, now() - interval '6 days', now() - interval '6 days');

  -- Order 6 (IN, PLACED, COD)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000006', v_p15, 'Free Range Eggs',    1, 6.50, 6.50, now() - interval '3 days', now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000006', v_p1,  'Organic Carrots',    1, 2.80, 2.80, now() - interval '3 days', now() - interval '3 days');

  -- Order 7 (IN, CANCELLED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000007', v_p14, 'Pomegranate',        2, 8.80, 17.60, now() - interval '5 days', now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000007', v_p13, 'Green Apples',       2, 5.80, 11.60, now() - interval '5 days', now() - interval '5 days');

  -- Order 8 (IN, DELIVERED, UPI)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000008', v_p16, 'Strawberries',       2, 9.90, 19.80, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000008', v_p2,  'Vine Tomatoes',      1, 3.90, 3.90, now() - interval '2 days', now() - interval '2 days');

  -- Order 9 (IN, CONFIRMED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000009', v_p17, 'Herbal Green Tea',   1, 7.20, 7.20, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000009', v_p18, 'Almonds',            1, 15.90, 15.90, now() - interval '1 day', now() - interval '1 day');

  -- Order 10 (IN, PLACED, UPI)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000010', v_p1,  'Organic Carrots',    2, 2.80, 5.60, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000010', v_p2,  'Vine Tomatoes',      1, 3.90, 3.90, now() - interval '1 day', now() - interval '1 day');

  -- Order 11 (SG, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000011', v_p6,  'Alphonso Mango',     2, 12.00, 24.00, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000011', v_p12, 'Broccoli Head',      3, 3.50, 10.50, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000011', v_p4,  'Bell Peppers Mix',   2, 4.50, 9.00, now() - interval '13 days', now() - interval '13 days');

  -- Order 12 (SG, DELIVERED, PayNow)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000012', v_p16, 'Strawberries',       2, 9.90, 19.80, now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000012', v_p3,  'Baby Spinach',       1, 3.20, 3.20, now() - interval '12 days', now() - interval '12 days');

  -- Order 13 (SG, DELIVERED, GrabPay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000013', v_p14, 'Pomegranate',        3, 8.80, 26.40, now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000013', v_p9,  'Honey (Raw Wildflower)', 2, 14.90, 29.80, now() - interval '10 days', now() - interval '10 days');

  -- Order 14 (SG, PROCESSING, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000014', v_p18, 'Almonds',            3, 15.90, 47.70, now() - interval '7 days', now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000014', v_p19, 'Dragon Fruit',       2, 7.50, 15.00, now() - interval '7 days', now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000014', v_p7,  'Coconut Water',      3, 4.50, 13.50, now() - interval '7 days', now() - interval '7 days');

  -- Order 15 (SG, SHIPPED, PayNow)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000015', v_p5,  'Fresh Bananas',      4, 3.20, 12.80, now() - interval '5 days', now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000015', v_p13, 'Green Apples',       2, 5.80, 11.60, now() - interval '5 days', now() - interval '5 days');

  -- Order 16 (SG, PLACED, GrabPay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000016', v_p1,  'Organic Carrots',    3, 2.80, 8.40, now() - interval '3 days', now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000016', v_p2,  'Vine Tomatoes',      2, 3.90, 7.80, now() - interval '3 days', now() - interval '3 days');

  -- Order 17 (SG, CANCELLED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000017', v_p9,  'Honey (Raw Wildflower)', 2, 14.90, 29.80, now() - interval '4 days', now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000017', v_p8,  'Basmati Rice',       1, 9.80, 9.80, now() - interval '4 days', now() - interval '4 days');

  -- Order 18 (SG, DELIVERED, PayNow)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000018', v_p11, 'Cherry Tomatoes',    2, 4.20, 8.40, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000018', v_p15, 'Free Range Eggs',    2, 6.50, 13.00, now() - interval '2 days', now() - interval '2 days');

  -- Order 19 (SG, PROCESSING, GrabPay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000019', v_p6,  'Alphonso Mango',     3, 12.00, 36.00, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000019', v_p16, 'Strawberries',       2, 9.90, 19.80, now() - interval '1 day', now() - interval '1 day');

  -- Order 20 (SG, CONFIRMED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000020', v_p20, 'Masala Chai',        1, 5.50, 5.50, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000020', v_p8,  'Basmati Rice',       1, 9.80, 9.80, now() - interval '1 day', now() - interval '1 day');

  -- Order 21 (US, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000021', v_p14, 'Pomegranate',        2, 8.80, 17.60, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000021', v_p13, 'Green Apples',       2, 5.80, 11.60, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000021', v_p5,  'Fresh Bananas',      3, 3.20, 9.60, now() - interval '13 days', now() - interval '13 days');

  -- Order 22 (US, DELIVERED, Apple Pay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000022', v_p18, 'Almonds',            2, 15.90, 31.80, now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000022', v_p16, 'Strawberries',       2, 9.90, 19.80, now() - interval '11 days', now() - interval '11 days');

  -- Order 23 (US, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000023', v_p6,  'Alphonso Mango',     1, 12.00, 12.00, now() - interval '9 days', now() - interval '9 days'),
    ('c1000000-0000-0000-0000-000000000023', v_p4,  'Bell Peppers Mix',   2, 4.50, 9.00, now() - interval '9 days', now() - interval '9 days'),
    ('c1000000-0000-0000-0000-000000000023', v_p8,  'Basmati Rice',       1, 9.80, 9.80, now() - interval '9 days', now() - interval '9 days');

  -- Order 24 (US, PROCESSING, Apple Pay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000024', v_p9,  'Honey (Raw Wildflower)', 3, 14.90, 44.70, now() - interval '7 days', now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000024', v_p10, 'Cold-Pressed Coconut Oil', 2, 14.50, 29.00, now() - interval '7 days', now() - interval '7 days');

  -- Order 25 (US, SHIPPED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000025', v_p12, 'Broccoli Head',      4, 3.50, 14.00, now() - interval '5 days', now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000025', v_p11, 'Cherry Tomatoes',    3, 4.20, 12.60, now() - interval '5 days', now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000025', v_p3,  'Baby Spinach',       4, 3.20, 12.80, now() - interval '5 days', now() - interval '5 days');

  -- Order 26 (US, PLACED, Apple Pay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000026', v_p15, 'Free Range Eggs',    2, 6.50, 13.00, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000026', v_p1,  'Organic Carrots',    2, 2.80, 5.60, now() - interval '2 days', now() - interval '2 days');

  -- Order 27 (US, CANCELLED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000027', v_p19, 'Dragon Fruit',       3, 7.50, 22.50, now() - interval '4 days', now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000027', v_p7,  'Coconut Water',      2, 4.50, 9.00, now() - interval '4 days', now() - interval '4 days');

  -- Order 28 (US, DELIVERED, Apple Pay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000028', v_p2,  'Vine Tomatoes',      3, 3.90, 11.70, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000028', v_p5,  'Fresh Bananas',      4, 3.20, 12.80, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000028', v_p17, 'Herbal Green Tea',   2, 7.20, 14.40, now() - interval '2 days', now() - interval '2 days');

  -- Order 29 (US, CONFIRMED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000029', v_p6,  'Alphonso Mango',     2, 12.00, 24.00, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000029', v_p8,  'Basmati Rice',       2, 9.80, 19.60, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000029', v_p20, 'Masala Chai',        2, 5.50, 11.00, now() - interval '1 day', now() - interval '1 day');

  -- Order 30 (US, PLACED, Apple Pay)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000030', v_p1,  'Organic Carrots',    3, 2.80, 8.40, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000030', v_p13, 'Green Apples',       2, 5.80, 11.60, now() - interval '1 day', now() - interval '1 day');

  -- Order 31 (AE, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000031', v_p6,  'Alphonso Mango',     3, 12.00, 36.00, now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000031', v_p9,  'Honey (Raw Wildflower)', 1, 14.90, 14.90, now() - interval '13 days', now() - interval '13 days');

  -- Order 32 (AE, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000032', v_p16, 'Strawberries',       2, 9.90, 19.80, now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000032', v_p4,  'Bell Peppers Mix',   2, 4.50, 9.00, now() - interval '12 days', now() - interval '12 days');

  -- Order 33 (AE, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000033', v_p18, 'Almonds',            3, 15.90, 47.70, now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000033', v_p8,  'Basmati Rice',       2, 9.80, 19.60, now() - interval '10 days', now() - interval '10 days');

  -- Order 34 (AE, PROCESSING, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000034', v_p14, 'Pomegranate',        4, 8.80, 35.20, now() - interval '7 days', now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000034', v_p19, 'Dragon Fruit',       3, 7.50, 22.50, now() - interval '7 days', now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000034', v_p10, 'Cold-Pressed Coconut Oil', 2, 14.50, 29.00, now() - interval '7 days', now() - interval '7 days');

  -- Order 35 (AE, SHIPPED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000035', v_p12, 'Broccoli Head',      3, 3.50, 10.50, now() - interval '6 days', now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000035', v_p11, 'Cherry Tomatoes',    2, 4.20, 8.40, now() - interval '6 days', now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000035', v_p3,  'Baby Spinach',       3, 3.20, 9.60, now() - interval '6 days', now() - interval '6 days');

  -- Order 36 (AE, PLACED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000036', v_p5,  'Fresh Bananas',      3, 3.20, 9.60, now() - interval '3 days', now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000036', v_p2,  'Vine Tomatoes',      2, 3.90, 7.80, now() - interval '3 days', now() - interval '3 days');

  -- Order 37 (AE, CANCELLED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000037', v_p6,  'Alphonso Mango',     1, 12.00, 12.00, now() - interval '4 days', now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000037', v_p1,  'Organic Carrots',    3, 2.80, 8.40, now() - interval '4 days', now() - interval '4 days');

  -- Order 38 (AE, DELIVERED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000038', v_p7,  'Coconut Water',      4, 4.50, 18.00, now() - interval '2 days', now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000038', v_p15, 'Free Range Eggs',    2, 6.50, 13.00, now() - interval '2 days', now() - interval '2 days');

  -- Order 39 (AE, CONFIRMED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000039', v_p17, 'Herbal Green Tea',   2, 7.20, 14.40, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000039', v_p20, 'Masala Chai',        2, 5.50, 11.00, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000039', v_p18, 'Almonds',            2, 15.90, 31.80, now() - interval '1 day', now() - interval '1 day');

  -- Order 40 (AE, PLACED, Card)
  INSERT INTO order_items (order_id, product_id, name, qty, unit_price, line_total, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000040', v_p13, 'Green Apples',       2, 5.80, 11.60, now() - interval '1 day', now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000040', v_p4,  'Bell Peppers Mix',   2, 4.50, 9.00, now() - interval '1 day', now() - interval '1 day');

  -- ============================================================
  -- 5. PAYMENTS (one per order)
  -- ============================================================

  -- India payments
  INSERT INTO payments (order_id, amount, method, status, paid_at, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000001', 15.63, 'Card',      'SUCCESSFUL', now() - interval '13 days', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000002', 25.42, 'UPI',       'SUCCESSFUL', now() - interval '12 days', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000003', 37.20, 'COD',       'SUCCESSFUL', now() - interval '11 days', now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000004', 21.95, 'Card',      'PENDING',    NULL,                           now() - interval '8 days',  now() - interval '8 days'),
    ('c1000000-0000-0000-0000-000000000005', 41.20, 'UPI',       'SUCCESSFUL', now() - interval '6 days',  now() - interval '6 days',  now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000006', 12.68, 'COD',       'PENDING',    NULL,                           now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000007', 33.50, 'Card',      'REFUNDED',   now() - interval '5 days',  now() - interval '5 days',  now() - interval '4 days'),
    ('c1000000-0000-0000-0000-000000000008', 19.00, 'UPI',       'SUCCESSFUL', now() - interval '2 days',  now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000009', 19.13, 'Card',      'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000010', 14.21, 'UPI',       'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day');

  -- Singapore payments
  INSERT INTO payments (order_id, amount, method, status, paid_at, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000011', 53.15, 'Card',      'SUCCESSFUL', now() - interval '13 days', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000012', 31.30, 'PayNow',    'SUCCESSFUL', now() - interval '12 days', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000013', 60.89, 'GrabPay',   'SUCCESSFUL', now() - interval '10 days', now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000014', 95.23, 'Card',      'PENDING',    NULL,                           now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000015', 34.24, 'PayNow',    'SUCCESSFUL', now() - interval '5 days',  now() - interval '5 days',  now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000016', 25.15, 'GrabPay',   'PENDING',    NULL,                           now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000017', 45.81, 'Card',      'REFUNDED',   now() - interval '4 days',  now() - interval '4 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000018', 26.54, 'PayNow',    'SUCCESSFUL', now() - interval '2 days',  now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000019', 65.00, 'GrabPay',   'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000020', 21.40, 'Card',      'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day');

  -- US payments
  INSERT INTO payments (order_id, amount, method, status, paid_at, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000021', 54.58, 'Card',      'SUCCESSFUL', now() - interval '13 days', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000022', 50.54, 'Apple Pay', 'SUCCESSFUL', now() - interval '11 days', now() - interval '11 days', now() - interval '11 days'),
    ('c1000000-0000-0000-0000-000000000023', 43.29, 'Card',      'SUCCESSFUL', now() - interval '9 days',  now() - interval '9 days',  now() - interval '9 days'),
    ('c1000000-0000-0000-0000-000000000024', 72.51, 'Apple Pay', 'PENDING',    NULL,                           now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000025', 57.28, 'Card',      'SUCCESSFUL', now() - interval '5 days',  now() - interval '5 days',  now() - interval '5 days'),
    ('c1000000-0000-0000-0000-000000000026', 32.30, 'Apple Pay', 'PENDING',    NULL,                           now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000027', 32.97, 'Card',      'REFUNDED',   now() - interval '4 days',  now() - interval '4 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000028', 64.62, 'Apple Pay', 'SUCCESSFUL', now() - interval '2 days',  now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000029', 59.94, 'Card',      'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000030', 27.44, 'Apple Pay', 'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day');

  -- UAE payments
  INSERT INTO payments (order_id, amount, method, status, paid_at, created_at, updated_at) VALUES
    ('c1000000-0000-0000-0000-000000000031', 67.75, 'Card',      'SUCCESSFUL', now() - interval '13 days', now() - interval '13 days', now() - interval '13 days'),
    ('c1000000-0000-0000-0000-000000000032', 35.91, 'Card',      'SUCCESSFUL', now() - interval '12 days', now() - interval '12 days', now() - interval '12 days'),
    ('c1000000-0000-0000-0000-000000000033', 78.04, 'Card',      'SUCCESSFUL', now() - interval '10 days', now() - interval '10 days', now() - interval '10 days'),
    ('c1000000-0000-0000-0000-000000000034', 99.75, 'Card',      'PENDING',    NULL,                           now() - interval '7 days',  now() - interval '7 days'),
    ('c1000000-0000-0000-0000-000000000035', 60.93, 'Card',      'SUCCESSFUL', now() - interval '6 days',  now() - interval '6 days',  now() - interval '6 days'),
    ('c1000000-0000-0000-0000-000000000036', 36.25, 'Card',      'PENDING',    NULL,                           now() - interval '3 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000037', 31.19, 'Card',      'REFUNDED',   now() - interval '4 days',  now() - interval '4 days',  now() - interval '3 days'),
    ('c1000000-0000-0000-0000-000000000038', 54.10, 'Card',      'SUCCESSFUL', now() - interval '2 days',  now() - interval '2 days',  now() - interval '2 days'),
    ('c1000000-0000-0000-0000-000000000039', 75.60, 'Card',      'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day'),
    ('c1000000-0000-0000-0000-000000000040', 31.00, 'Card',      'PENDING',    NULL,                           now() - interval '1 day',   now() - interval '1 day');

  -- ============================================================
  -- 6. TESTIMONIALS (additional for variety)
  -- ============================================================
  INSERT INTO testimonials (name, role, location, rating, quote)
  SELECT * FROM (VALUES
    ('Rajesh Kumar', 'Farmer', 'India', 5, 'VPNexues connects me directly with urban customers. Fresh produce, fair prices.'),
    ('Michelle Wong', 'Home Cook', 'Singapore', 4, 'The Small Box plan is perfect for my weekly meal prep. Always fresh.'),
    ('Ahmed Khalil', 'Restaurant Manager', 'UAE', 5, 'Bulk orders are always on time and the quality is consistent. Highly recommend.'),
    ('Sarah Mitchell', 'Nutritionist', 'USA', 4, 'I recommend VPNexues to all my clients for organic, farm-fresh produce.'),
    ('Deepa Menon', 'Food Blogger', 'India', 5, 'The Alphonso mangoes are the best I have ever tasted. Incredible quality!')
  ) AS t(name, role, location, rating, quote)
  WHERE NOT EXISTS (SELECT 1 FROM testimonials WHERE name = 'Rajesh Kumar');
END $$;
