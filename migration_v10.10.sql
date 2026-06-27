-- ============================================================
--  migration_v10.10.sql
--  Fix 1  (already in model — no DDL needed: customerId on
--          sale_transaction was added in a previous migration)
--  Fix 2  — premiumAmount on sale_transaction
--  Fix 4  — premiumAmount + discountAmount on purchase
-- ============================================================
--
--  NOTE: Hibernate ddl-auto=update will create these columns
--        automatically on first start.  Run this script on
--        databases managed outside Hibernate (e.g. production).
--
--  Safe to run multiple times — uses IF NOT EXISTS / IGNORE.
-- ============================================================

-- ── Fix 2: sale_transaction.premium_amount ──────────────────
ALTER TABLE sale_transaction
    ADD COLUMN IF NOT EXISTS premium_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00
    COMMENT 'Total premium (positive) or discount (negative) in AED for this transaction, stored from the calculator';

-- ── Fix 4: purchase.premium_amount ──────────────────────────
ALTER TABLE purchase
    ADD COLUMN IF NOT EXISTS premium_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00
    COMMENT 'Total premium in AED summed from per-item calculator premiums';

-- ── Fix 4: purchase.discount_amount ─────────────────────────
ALTER TABLE purchase
    ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00
    COMMENT 'Total discount in AED summed from per-item calculator discounts';

-- ── Fix: purchase.status — widen MySQL ENUM → VARCHAR(20) ───
--  Hibernate 6 + MySQL creates @Enumerated(STRING) columns as
--  ENUM types.  Adding RETURNED to the Java enum requires the
--  column to accept the new value.  Converting to VARCHAR(20)
--  is safe: all existing values ('FULLY_PAID','PARTIALLY_PAID',
--  'DUE') are preserved and any future enum values work without
--  another migration.
ALTER TABLE purchase
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'FULLY_PAID';

-- ── Fix: business.receipt_logo_base64 — widen TEXT → MEDIUMTEXT ─
--  TEXT is only 64 KB; a base64-encoded logo can easily exceed that.
--  MEDIUMTEXT holds up to 16 MB, which is sufficient for any logo.
ALTER TABLE business
    MODIFY COLUMN receipt_logo_base64 MEDIUMTEXT;

-- ── Fix: fix_settlement.settlement_market_rate — new column ──
--  Stores the raw market rate entered by the cashier at settlement time.
--  fixedRate on the same row now stores the EFFECTIVE rate (mkt ± disc/prem).
--  NULL for existing rows (pre-fix data lacked this distinction).
ALTER TABLE fix_settlement
    ADD COLUMN IF NOT EXISTS settlement_market_rate DECIMAL(10,2) NULL
    COMMENT 'Raw market rate ($/oz) entered at settlement before reapplying deal discount/premium';

-- ── Fix: business.expiry_date — clear expired dates ─────────
--  Subscription enforcement has been disabled in code.
--  This removes the expiry gate from any existing business rows
--  so login works immediately without a Java restart.
UPDATE business SET expiry_date = NULL WHERE expiry_date IS NOT NULL;

-- ============================================================
--  End of migration_v10.10.sql
-- ============================================================
