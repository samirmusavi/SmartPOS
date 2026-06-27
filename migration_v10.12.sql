-- ─────────────────────────────────────────────────────────────
-- SmartPOS v10.12 Migration
-- Adds product_class column to product table.
-- Run once on existing databases; Hibernate ddl-auto=update
-- will handle brand-new databases automatically.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS product_class VARCHAR(20) DEFAULT 'JEWELLERY';

-- Backfill: ensure no NULLs remain for existing rows
UPDATE product SET product_class = 'JEWELLERY' WHERE product_class IS NULL;
