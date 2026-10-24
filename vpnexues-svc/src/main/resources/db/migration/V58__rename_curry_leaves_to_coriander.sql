-- giri-yz renamed the herb "Curry Leaves" -> "Coriander" (slug intentionally kept
-- 'curry-leaves'). That rename was made by editing R__Seed_data.sql and the
-- already-applied V7__product_images_local.sql, neither of which can change live
-- rows: V7 never re-runs, and R__Seed_data.sql inserts with ON CONFLICT DO NOTHING
-- (the existing row wins). This versioned migration is the piece that actually
-- applies the rename to existing production data. Idempotent.
--
-- image_url uses the Cloudinary asset (uploaded 23-Oct-2026, public_id
-- vpnexues/Coriander, same cloud/folder as every other live product image)
-- instead of a local /images/products path, so the row is served from the CDN
-- like the rest of the catalog.
UPDATE products
   SET name = 'Coriander',
       image_url = 'https://res.cloudinary.com/eqjshlur/image/upload/v1792728849/vpnexues/Coriander.jpg'
 WHERE slug = 'curry-leaves';
