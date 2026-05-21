-- SmartPOS Seed Data Script (idempotent)

-- 1. Set ALL user passwords to 'Test1234'
UPDATE app_user SET password = '$2a$10$KfTEXaA3S43Z3PoAOhvnaeEe6f8fENHs7rWqy5IINkPGBVy6E.rRO';

-- 2. Upgrade Demo Business to ENTERPRISE
UPDATE business SET plan = 'ENTERPRISE', expiry_date = '2027-12-31 23:59:59' WHERE id = 4;

-- 3. Clean up any partial inserts from previous runs for business 4
DELETE FROM branch_inventory WHERE branch_id IN (SELECT id FROM branch WHERE business_id = 4);
DELETE FROM branch_supply_inventory WHERE branch_id IN (SELECT id FROM branch WHERE business_id = 4);
DELETE FROM supplier WHERE business_id = 4;
DELETE FROM product WHERE business_id = 4;
DELETE FROM branch WHERE business_id = 4;

-- 4. Create branches
INSERT INTO branch (business_id, name, city, country, is_main_branch, status, created_at, updated_at)
VALUES (4, 'Main Branch', 'Dubai', 'UAE', true, 'ACTIVE', NOW(), NOW());
SET @b1 = LAST_INSERT_ID();

INSERT INTO branch (business_id, name, city, country, is_main_branch, status, created_at, updated_at)
VALUES (4, 'Abu Dhabi Branch', 'Abu Dhabi', 'UAE', false, 'ACTIVE', NOW(), NOW());
SET @b2 = LAST_INSERT_ID();

-- 5. Products
INSERT INTO product (business_id, name, barcode, price, cost_price, quantity, category, active, created_at, updated_at) VALUES
(4, 'iPhone 16 Pro Max',  'IP16PM001', 5499.00, 4200.00, 25, 'Electronics', true, NOW(), NOW()),
(4, 'Samsung Galaxy S25', 'SGS25001',  4299.00, 3100.00, 30, 'Electronics', true, NOW(), NOW()),
(4, 'AirPods Pro 2',      'APP2001',   999.00,  650.00,  50, 'Electronics', true, NOW(), NOW()),
(4, 'MacBook Air M3',     'MBA3001',   4999.00, 3800.00, 10, 'Electronics', true, NOW(), NOW()),
(4, 'Nivea Body Lotion',  'NVA001',    45.00,   22.00,  100, 'Personal Care', true, NOW(), NOW()),
(4, 'Coca Cola 330ml',    'CC330001',   3.50,    1.50,  500, 'Beverages', true, NOW(), NOW()),
(4, 'Red Bull 250ml',     'RB250001',   8.00,    4.50,  200, 'Beverages', true, NOW(), NOW()),
(4, 'Almarai Milk 1L',    'ALM1L001',   7.50,    4.00,  150, 'Dairy', true, NOW(), NOW()),
(4, 'Arabic Coffee 250g', 'ARC250001', 35.00,   18.00,   80, 'Beverages', true, NOW(), NOW()),
(4, 'USB-C Cable 1m',     'USBC1M001',  25.00,  8.00,   200, 'Accessories', true, NOW(), NOW());

-- 6. Branch inventory
INSERT INTO branch_inventory (branch_id, product_id, quantity)
SELECT @b1, p.id, p.quantity FROM product p WHERE p.business_id = 4;

INSERT INTO branch_inventory (branch_id, product_id, quantity)
SELECT @b2, p.id, 0 FROM product p WHERE p.business_id = 4;

-- 7. Suppliers
INSERT INTO supplier (business_id, name, contact_person, email, phone, created_at, updated_at) VALUES
(4, 'UAE Electronics LLC', 'Ahmed Al Maktoum', 'ahmed@uaeelec.ae', '+971501111111', NOW(), NOW()),
(4, 'Gulf Distribution Co', 'Sara Hassan', 'sara@gulfdist.ae', '+971502222222', NOW(), NOW());

-- 8. Verify
SELECT 'OK' AS status, COUNT(*) AS businesses FROM business;
SELECT 'OK' AS status, COUNT(*) AS users FROM app_user;
SELECT 'OK' AS status, COUNT(*) AS branches FROM branch WHERE business_id = 4;
SELECT 'OK' AS status, COUNT(*) AS products FROM product WHERE business_id = 4;
SELECT 'OK' AS status, COUNT(*) AS inventory_rows FROM branch_inventory WHERE branch_id IN (SELECT id FROM branch WHERE business_id = 4);
