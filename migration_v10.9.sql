-- ============================================================
-- SmartPOS v10.9 Migration
-- Party Ledger + Metal Fix (HPF) feature
-- Run once on smartpos_db in MySQL Workbench
-- ============================================================

-- ── 1. Add party_code column to supplier ──────────────────────
-- Already applied manually — column exists, skip.
-- ALTER TABLE `supplier`
--     ADD COLUMN `party_code` VARCHAR(20) NULL AFTER `kyc_notes`;

-- Index for fast lookup by party code within a business
CREATE INDEX `idx_supplier_party_code`
    ON `supplier` (`business_id`, `party_code`);

-- ── 2. Voucher sequence table ─────────────────────────────────
-- Holds the last-used sequence number for each voucher type per business.
-- Pessimistic lock in VoucherSequenceRepository prevents duplicates.
CREATE TABLE IF NOT EXISTS `voucher_sequence` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_id`  BIGINT       NOT NULL,
    `voucher_type` VARCHAR(10)  NOT NULL,
    `last_number`  INT          NOT NULL DEFAULT 0,
    UNIQUE KEY `uq_vs_business_type` (`business_id`, `voucher_type`)
);

-- ── 3. Party ledger entry table ───────────────────────────────
-- One row per financial event posted to a party's ledger.
-- Running balance is computed in Java; this table stores raw debits/credits.
CREATE TABLE IF NOT EXISTS `party_ledger_entry` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_id`    BIGINT          NOT NULL,
    `branch_id`      BIGINT          NULL,
    `party_id`       BIGINT          NOT NULL,
    `party_name`     VARCHAR(255)    NULL,
    `voucher_type`   VARCHAR(10)     NOT NULL     COMMENT 'SAL / PUR / REC / PAY / HPF / SFX / ADJ',
    `voucher_number` VARCHAR(100)    NULL,
    `voucher_date`   DATE            NOT NULL,
    `narration`      VARCHAR(500)    NULL,
    `aed_debit`      DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    `aed_credit`     DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    `metal_debit`    DOUBLE          NOT NULL DEFAULT 0,
    `metal_credit`   DOUBLE          NOT NULL DEFAULT 0,
    `metal_type`     VARCHAR(10)     NULL         COMMENT 'GOLD / SILVER',
    `purity`         VARCHAR(20)     NULL,
    `reference_id`   BIGINT          NULL,
    `reference_type` VARCHAR(50)     NULL         COMMENT 'SALE / PURCHASE / RECEIPT / PAYMENT / FIX / SETTLEMENT',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by`     VARCHAR(100)    NULL,

    INDEX `idx_ple_party`  (`business_id`, `party_id`),
    INDEX `idx_ple_date`   (`business_id`, `party_id`, `voucher_date`)
);

-- ── 4. Metal fix table ────────────────────────────────────────
-- One row per Hold-Price Fix (HPF) agreement.
-- Lifecycle: OPEN → FIXED → SETTLED
CREATE TABLE IF NOT EXISTS `metal_fix` (
    `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_id`           BIGINT          NOT NULL,
    `branch_id`             BIGINT          NULL,
    `fix_number`            VARCHAR(50)     NOT NULL UNIQUE  COMMENT 'HPF-{SEQ}',
    `party_id`              BIGINT          NOT NULL,
    `party_name`            VARCHAR(255)    NULL,
    `fix_type`              VARCHAR(20)     NOT NULL         COMMENT 'SALE_FIX / PURCHASE_FIX',
    `metal_type`            VARCHAR(10)     NOT NULL         COMMENT 'GOLD / SILVER',
    `purity`                VARCHAR(20)     NULL,
    `weight_grams`          DOUBLE          NOT NULL,
    `transaction_rate`      DECIMAL(10,2)   NOT NULL         COMMENT 'USD per troy oz at time of deal',
    `discount_premium`      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    `discount_premium_type` VARCHAR(20)     NOT NULL DEFAULT 'PER_OZ'   COMMENT 'PER_OZ / PERCENTAGE',
    `effective_rate`        DECIMAL(10,2)   NULL,
    `exchange_rate`         DECIMAL(10,4)   NOT NULL DEFAULT 3.6740,
    `total_aed`             DECIMAL(15,2)   NULL,
    `margin_percent`        DECIMAL(5,2)    NULL,
    `margin_amount`         DECIMAL(15,2)   NULL,
    `status`                VARCHAR(20)     NOT NULL DEFAULT 'OPEN'  COMMENT 'OPEN / FIXED / SETTLED',
    `fixed_rate`            DECIMAL(10,2)   NULL,
    `fixed_date`            DATE            NULL,
    `settlement_amount`     DECIMAL(15,2)   NULL,
    `settlement_date`       DATE            NULL,
    `linked_sale_id`        BIGINT          NULL,
    `linked_purchase_id`    BIGINT          NULL,
    `notes`                 TEXT            NULL,
    `created_at`            DATETIME        DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`            VARCHAR(100)    NULL,

    INDEX `idx_mf_business_party`  (`business_id`, `party_id`),
    INDEX `idx_mf_business_status` (`business_id`, `status`)
);

-- ── 5. Fix settlement table ───────────────────────────────────
-- One row per settlement of a MetalFix.
-- Records the final rate and the AED difference (CR/DR) vs the original HPF estimate.
CREATE TABLE IF NOT EXISTS `fix_settlement` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `metal_fix_id`      BIGINT          NOT NULL,
    `settlement_number` VARCHAR(50)     NOT NULL UNIQUE   COMMENT 'SFX-{SEQ}',
    `business_id`       BIGINT          NOT NULL,
    `branch_id`         BIGINT          NULL,
    `party_id`          BIGINT          NOT NULL,
    `party_name`        VARCHAR(255)    NULL,
    `weight_grams`      DOUBLE          NOT NULL,
    `fixed_rate`        DECIMAL(10,2)   NOT NULL,
    `exchange_rate`     DECIMAL(10,4)   NOT NULL DEFAULT 3.6740,
    `settlement_aed`    DECIMAL(15,2)   NOT NULL,
    `original_aed`      DECIMAL(15,2)   NOT NULL,
    `difference_cr_dr`  DECIMAL(15,2)   NULL     COMMENT '+ve = CR for party, -ve = DR for party',
    `settled_by`        VARCHAR(100)    NULL,
    `settlement_date`   DATE            NOT NULL,
    `notes`             TEXT            NULL,
    `created_at`        DATETIME        DEFAULT CURRENT_TIMESTAMP,

    INDEX `idx_fs_fix`        (`metal_fix_id`),
    INDEX `idx_fs_party_date` (`business_id`, `party_id`, `settlement_date`)
);
