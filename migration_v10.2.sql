-- Migration Script: Phase 1 - Gold/Silver Bullion Updates
-- These alters are required because Hibernate's ddl-auto=update cannot safely convert INT to DOUBLE without this step.

-- 1. Product Inventory
ALTER TABLE product MODIFY COLUMN quantity DOUBLE NOT NULL DEFAULT 0;

-- 2. Branch Inventory
ALTER TABLE branch_inventory MODIFY COLUMN quantity DOUBLE NOT NULL DEFAULT 0;

-- 3. Sale Items
ALTER TABLE sale MODIFY COLUMN quantity_sold DOUBLE NOT NULL DEFAULT 0;

-- 4. Sale Returns
ALTER TABLE sale_return MODIFY COLUMN quantity_returned DOUBLE NOT NULL DEFAULT 0;

-- 5. Stock Transfers
ALTER TABLE stock_transfer MODIFY COLUMN quantity DOUBLE NOT NULL DEFAULT 0;

-- 6. Stock Adjustments
ALTER TABLE stock_adjustment MODIFY COLUMN old_quantity DOUBLE NOT NULL DEFAULT 0;
ALTER TABLE stock_adjustment MODIFY COLUMN new_quantity DOUBLE NOT NULL DEFAULT 0;
ALTER TABLE stock_adjustment MODIFY COLUMN adjustment DOUBLE NOT NULL DEFAULT 0;

-- 7. Purchase Orders
ALTER TABLE purchase_order MODIFY COLUMN quantity_ordered DOUBLE NOT NULL DEFAULT 0;
ALTER TABLE purchase_order MODIFY COLUMN quantity_received DOUBLE DEFAULT NULL;

-- ── Phase 2+ additions ─────────────────────────────────────────

-- 8. New columns on sale_transaction (Phase 2)
ALTER TABLE sale_transaction
  ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS due_amount      DECIMAL(10,2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS vat_percent     DOUBLE        DEFAULT 5.0,
  ADD COLUMN IF NOT EXISTS status          VARCHAR(50)   DEFAULT 'FULLY_PAID',
  ADD COLUMN IF NOT EXISTS exchange_rate   DECIMAL(10,4) NULL,
  ADD COLUMN IF NOT EXISTS pricing_method  VARCHAR(50)   NULL,
  ADD COLUMN IF NOT EXISTS gold_oz_rate    DECIMAL(10,2) NULL,
  ADD COLUMN IF NOT EXISTS customer_id     BIGINT        NULL,
  ADD COLUMN IF NOT EXISTS customer_name   VARCHAR(255)  NULL;

-- 9. Return invoice number and transaction link (Phase 2/4)
ALTER TABLE sale_return
  ADD COLUMN IF NOT EXISTS invoice_number  VARCHAR(255) NULL,
  ADD COLUMN IF NOT EXISTS transaction_id  BIGINT       NULL;

-- 10. Supplier / customer dual-role link columns (Phase 6)
ALTER TABLE customer ADD COLUMN IF NOT EXISTS linked_supplier_id BIGINT NULL;
ALTER TABLE supplier ADD COLUMN IF NOT EXISTS linked_customer_id BIGINT NULL;

-- 11. Back-fill status for existing transactions
UPDATE sale_transaction
SET status = 'FULLY_PAID'
WHERE status IS NULL;
