-- ============================================================
-- SmartPOS v10.4 Migration — Hand Loan entry types
-- Run this once in MySQL Workbench on smartpos_db
-- ============================================================

-- The cash_entry table was created with entry_type as an ENUM.
-- Add HAND_LOAN_IN and HAND_LOAN_OUT to the allowed values.

ALTER TABLE `cash_entry`
    MODIFY COLUMN `entry_type`
    ENUM('SALE','EXPENSE','RETURN','MANUAL_IN','MANUAL_OUT','OPENING','HAND_LOAN_IN','HAND_LOAN_OUT')
    COLLATE utf8mb4_unicode_ci NOT NULL;
