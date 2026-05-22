-- ============================================================
-- SmartPOS v10.3 Migration — Cash Sheet feature
-- Run this once in MySQL Workbench on smartpos_db
-- ============================================================

-- ── Cash Entry ───────────────────────────────────────────────
-- One row per cash-affecting event (sale, expense, return, manual)
CREATE TABLE IF NOT EXISTS cash_entry (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_id  BIGINT       NOT NULL,
    branch_id    BIGINT,
    sheet_date   DATE         NOT NULL,
    party_name   VARCHAR(255),
    description  VARCHAR(500),
    amount_in    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    amount_out   DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    entry_type   VARCHAR(50)  NOT NULL,   -- SALE | EXPENSE | RETURN | MANUAL_IN | MANUAL_OUT | OPENING
    reference_id BIGINT,                  -- FK to originating record (sale_transaction / expense / sale_return)
    sort_order   INT          NOT NULL DEFAULT 0,
    created_by   VARCHAR(255),
    created_at   DATETIME     NOT NULL DEFAULT NOW(),

    INDEX idx_ce_business_branch_date (business_id, branch_id, sheet_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Cash Day Sheet ───────────────────────────────────────────
-- One row per business + branch + date. Stores opening balance
-- and the physical denomination count done at end of day.
CREATE TABLE IF NOT EXISTS cash_day_sheet (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_id         BIGINT        NOT NULL,
    branch_id           BIGINT,
    sheet_date          DATE          NOT NULL,
    opening_balance     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    opening_overridden  BOOLEAN       NOT NULL DEFAULT FALSE,
    bundles_1000        INT           NOT NULL DEFAULT 0,
    bundles_500         INT           NOT NULL DEFAULT 0,
    bundles_200         INT           NOT NULL DEFAULT 0,
    bundles_100         INT           NOT NULL DEFAULT 0,
    bundles_50          INT           NOT NULL DEFAULT 0,
    bundles_10          INT           NOT NULL DEFAULT 0,
    bundles_5           INT           NOT NULL DEFAULT 0,
    mix_notes           DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    coins               DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    hand_loan_total     DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at          DATETIME      NOT NULL DEFAULT NOW(),
    updated_at          DATETIME      ON UPDATE NOW(),

    -- Only one sheet per business+branch+date (branch_id may be NULL for legacy;
    -- MySQL treats NULL as distinct in UNIQUE so non-null rows are enforced correctly)
    UNIQUE KEY uq_cash_day (business_id, branch_id, sheet_date),
    INDEX idx_cds_business_branch (business_id, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
