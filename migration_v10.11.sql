-- ─────────────────────────────────────────────────────────────
-- SmartPOS v10.11 Migration
-- Adds round-off and reverse-charge declaration columns to
-- sale_transaction and purchase tables.
-- Run once on existing databases; Hibernate ddl-auto=update
-- will handle brand-new databases automatically.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE sale_transaction
    ADD COLUMN IF NOT EXISTS round_off_amount DECIMAL(10,2) DEFAULT 0;

ALTER TABLE sale_transaction
    ADD COLUMN IF NOT EXISTS include_reverse_charge_declaration BOOLEAN DEFAULT FALSE;

ALTER TABLE purchase
    ADD COLUMN IF NOT EXISTS round_off_amount DECIMAL(10,2) DEFAULT 0;
