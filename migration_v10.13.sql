-- migration_v10.13.sql
-- Stage 2: Add Unfixed-pricing fields to sale_transaction and purchase
--
-- original_pricing_method:
--   FIXED_AT_TRADE   = normal transaction — AED settled immediately (default, existing rows)
--   UNFIXED_AT_TRADE = metal moves now, AED deferred until a later Fixing event
--
-- fixing_completion_status:
--   NOT_APPLICABLE   = FIXED_AT_TRADE transaction (default, existing rows)
--   OPEN             = UNFIXED_AT_TRADE with remaining open weight > 0
--   FULLY_FIXED      = all pure weight has been consumed by Fixing events
--
-- gross_weight_grams  : total gross grams across all line items
-- pure_weight_grams   : gross × (purity/1000) — always server-calculated
-- agreed_premium_discount : per-oz USD premium/discount locked at deal time
-- remaining_open_weight_grams : pure grams not yet fixed (decremented by each Fixing)

ALTER TABLE sale_transaction
    ADD COLUMN IF NOT EXISTS original_pricing_method      VARCHAR(20)    NOT NULL DEFAULT 'FIXED_AT_TRADE',
    ADD COLUMN IF NOT EXISTS fixing_completion_status     VARCHAR(20)    NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN IF NOT EXISTS gross_weight_grams           DOUBLE         NULL,
    ADD COLUMN IF NOT EXISTS pure_weight_grams            DOUBLE         NULL,
    ADD COLUMN IF NOT EXISTS agreed_premium_discount      DECIMAL(10,4)  NULL,
    ADD COLUMN IF NOT EXISTS remaining_open_weight_grams  DOUBLE         NULL;

ALTER TABLE purchase
    ADD COLUMN IF NOT EXISTS original_pricing_method      VARCHAR(20)    NOT NULL DEFAULT 'FIXED_AT_TRADE',
    ADD COLUMN IF NOT EXISTS fixing_completion_status     VARCHAR(20)    NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN IF NOT EXISTS gross_weight_grams           DOUBLE         NULL,
    ADD COLUMN IF NOT EXISTS pure_weight_grams            DOUBLE         NULL,
    ADD COLUMN IF NOT EXISTS agreed_premium_discount      DECIMAL(10,4)  NULL,
    ADD COLUMN IF NOT EXISTS remaining_open_weight_grams  DOUBLE         NULL;
