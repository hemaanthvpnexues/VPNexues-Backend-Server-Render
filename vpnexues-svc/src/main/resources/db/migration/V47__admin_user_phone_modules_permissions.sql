-- Sub-admin access control + contact phone.
--   phone       : contact number collected on the Add/Edit Sub Admin form.
--   modules     : comma-separated module keys the admin may access
--                 (dashboard, orders, products, users, reports, payments, support, content)
--   permissions : comma-separated action keys (view, edit, delete, export)
-- NULL/blank modules = legacy account created before this migration = full access
-- (backward compatible: existing sub-admins keep working until re-saved with grants).
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS modules VARCHAR(255);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS permissions VARCHAR(255);
