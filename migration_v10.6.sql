-- ============================================================
-- SmartPOS v10.6 Migration — Total weight tracking per product
-- Run this once in MySQL Workbench on smartpos_db
-- ============================================================

-- Track total weight (grams) per product per branch
ALTER TABLE `branch_inventory`
    ADD COLUMN IF NOT EXISTS `total_weight_grams` DOUBLE NULL;

-- Track global total weight (grams) per product (synced from branch_inventory)
ALTER TABLE `product`
    ADD COLUMN IF NOT EXISTS `total_weight_grams` DOUBLE NULL;
