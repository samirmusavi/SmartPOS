-- migration_v10.8.sql
-- Adds Gold Exchange payment detail columns to the purchase table.

ALTER TABLE `purchase` ADD COLUMN IF NOT EXISTS `gold_payment_purity`       VARCHAR(50)    NULL AFTER `notes`;
ALTER TABLE `purchase` ADD COLUMN IF NOT EXISTS `gold_payment_weight_grams`  DOUBLE         NULL AFTER `gold_payment_purity`;
ALTER TABLE `purchase` ADD COLUMN IF NOT EXISTS `gold_payment_oz_rate`       DECIMAL(10,2)  NULL AFTER `gold_payment_weight_grams`;
ALTER TABLE `purchase` ADD COLUMN IF NOT EXISTS `gold_payment_value`         DECIMAL(15,2)  NULL AFTER `gold_payment_oz_rate`;
ALTER TABLE `purchase` ADD COLUMN IF NOT EXISTS `cash_amount_paid`           DECIMAL(15,2)  NULL AFTER `gold_payment_value`;
