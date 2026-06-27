-- migration_v10.14.sql
-- Stage 3: Distinguish UNFIXED-position fixes from standalone Hold-Price fixes
--
-- linked_transaction_type:
--   NULL              = standalone HPF fix (existing behaviour — completely unchanged)
--   UNFIXED_SALE      = consuming open weight from an UNFIXED_AT_TRADE SaleTransaction
--   UNFIXED_PURCHASE  = consuming open weight from an UNFIXED_AT_TRADE Purchase
--
-- For UNFIXED_SALE / UNFIXED_PURCHASE fixes:
--   • linked_sale_id / linked_purchase_id point to the parent (already nullable columns, now enforced in code)
--   • discountPremium is locked from the parent transaction's agreedPremiumDiscount
--   • status is set to FIXED immediately (immutable — no OPEN→FIXED→SETTLED lifecycle)
--   • ledger entry posts AED only (metal was already posted at UNFIXED_AT_TRADE time)

ALTER TABLE metal_fix
    ADD COLUMN IF NOT EXISTS linked_transaction_type VARCHAR(20) NULL
        COMMENT 'NULL=standalone | UNFIXED_SALE | UNFIXED_PURCHASE';
