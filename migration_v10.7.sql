-- ============================================================
-- SmartPOS v10.7 Migration — Purchase Return feature
-- Run this once in MySQL Workbench on smartpos_db
-- ============================================================

CREATE TABLE IF NOT EXISTS `purchase_return` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_id`           BIGINT NOT NULL,
    `branch_id`             BIGINT NOT NULL,
    `purchase_id`           BIGINT NOT NULL,
    `supplier_name`         VARCHAR(255),
    `return_invoice_number` VARCHAR(100) NOT NULL UNIQUE,
    `total_return_amount`   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `reason`                VARCHAR(50)   NOT NULL DEFAULT 'OTHER',
    `notes`                 TEXT,
    `created_by`            VARCHAR(255),
    `returned_at`           DATETIME DEFAULT NOW(),
    CONSTRAINT fk_pr_purchase FOREIGN KEY (`purchase_id`) REFERENCES `purchase`(`id`)
);

CREATE TABLE IF NOT EXISTS `purchase_return_item` (
    `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    `purchase_return_id`   BIGINT       NOT NULL,
    `purchase_item_id`     BIGINT       NULL,
    `product_id`           BIGINT       NULL,
    `product_name`         VARCHAR(255),
    `purity`               VARCHAR(50),
    `unit_type`            VARCHAR(10)  NOT NULL DEFAULT 'PCS',
    `return_quantity`      DOUBLE       NOT NULL DEFAULT 0,
    `return_weight_grams`  DOUBLE       NULL,
    `unit_price`           DECIMAL(15,2),
    `total_return_amount`  DECIMAL(15,2),
    CONSTRAINT fk_pri_return FOREIGN KEY (`purchase_return_id`)
        REFERENCES `purchase_return`(`id`) ON DELETE CASCADE
);
