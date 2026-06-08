-- ============================================================
-- SmartPOS v10.5 Migration — Purchase feature + Scrap product
-- Run this once in MySQL Workbench on smartpos_db
-- ============================================================

-- ── 1. Purchase header table ──────────────────────────────────
CREATE TABLE IF NOT EXISTS `purchase` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `business_id`    BIGINT       NOT NULL,
    `branch_id`      BIGINT       NOT NULL,
    `supplier_id`    BIGINT       DEFAULT NULL,
    `supplier_name`  VARCHAR(255) DEFAULT NULL,
    `invoice_number` VARCHAR(100) NOT NULL UNIQUE,
    `purchase_date`  DATETIME(6)  NOT NULL,
    `payment_method` VARCHAR(30)  NOT NULL,
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'FULLY_PAID',
    `total_amount`   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `amount_paid`    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `due_amount`     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `exchange_rate`  DECIMAL(10,4) DEFAULT 3.6740,
    `notes`          VARCHAR(1000) DEFAULT NULL,
    `created_by`     VARCHAR(255) DEFAULT NULL,
    `created_at`     DATETIME(6)  DEFAULT NULL,
    `updated_at`     DATETIME(6)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_purchase_business_branch` (`business_id`, `branch_id`),
    KEY `idx_purchase_date`            (`purchase_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. Purchase line items table ──────────────────────────────
CREATE TABLE IF NOT EXISTS `purchase_item` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `purchase_id`    BIGINT        NOT NULL,
    `product_id`     BIGINT        DEFAULT NULL,
    `product_name`   VARCHAR(255)  NOT NULL,
    `purity`         VARCHAR(50)   DEFAULT NULL,
    `unit_type`      VARCHAR(10)   NOT NULL DEFAULT 'PCS',
    `quantity`       DOUBLE        NOT NULL DEFAULT 0,
    `weight_grams`   DOUBLE        DEFAULT NULL,
    `pricing_method` VARCHAR(20)   NOT NULL DEFAULT 'FIXED_PRICE',
    `gold_oz_rate`   DECIMAL(10,2) DEFAULT NULL,
    `price_per_gram` DECIMAL(10,4) DEFAULT NULL,
    `unit_price`     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `total_price`    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `scrap_purity`   VARCHAR(20)   DEFAULT NULL,
    `created_at`     DATETIME(6)   DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_purchase_item_purchase` (`purchase_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. Add PURCHASE to cash_entry entry_type ENUM ─────────────
-- (This also retains the HAND_LOAN values added in v10.4)
ALTER TABLE `cash_entry`
    MODIFY COLUMN `entry_type`
    ENUM('SALE','EXPENSE','RETURN','MANUAL_IN','MANUAL_OUT','OPENING',
         'HAND_LOAN_IN','HAND_LOAN_OUT','PURCHASE')
    COLLATE utf8mb4_unicode_ci NOT NULL;

-- ── 4. Add is_scrap flag to product table ─────────────────────
ALTER TABLE `product`
    ADD COLUMN IF NOT EXISTS `is_scrap` TINYINT(1) NOT NULL DEFAULT 0;

-- ── 5. Add scrap_purity to sale line item ─────────────────────
ALTER TABLE `sale`
    ADD COLUMN IF NOT EXISTS `scrap_purity` VARCHAR(20) DEFAULT NULL;
