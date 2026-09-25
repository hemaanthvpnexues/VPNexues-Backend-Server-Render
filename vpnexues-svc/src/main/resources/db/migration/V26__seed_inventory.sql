-- Seed starting stock for every product missing an inventory row.
-- Checkout hard-fails when no inventory_items row exists for a product, and
-- admin-created products default to quantity 0 — so every seed/re-seeded product
-- was returning "Insufficient stock for <name> (available: 0, requested: N)".
-- Give each product a generous starting quantity so orders can be placed.
INSERT INTO inventory_items (id, product_id, quantity, reorder_threshold)
SELECT gen_random_uuid(), p.id, 500, 10
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM inventory_items i WHERE i.product_id = p.id);
