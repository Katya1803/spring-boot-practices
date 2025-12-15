-- =============================================
-- SEED DATA: roles, permissions, admin user
-- =============================================

-- Insert Roles
INSERT INTO roles (code, name, description) VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Full system access'),
    ('BRANCH_MANAGER', 'Branch Manager', 'Manage assigned branches'),
    ('SALES_STAFF', 'Sales Staff', 'Create and manage orders'),
    ('WAREHOUSE_STAFF', 'Warehouse Staff', 'Manage inventory'),
    ('ACCOUNTANT', 'Accountant', 'View reports and financial data');

-- Insert Permissions
INSERT INTO permissions (code, name, module) VALUES
    -- User module
    ('user:view', 'View Users', 'user'),
    ('user:create', 'Create User', 'user'),
    ('user:update', 'Update User', 'user'),
    ('user:delete', 'Delete User', 'user'),

    -- Branch module
    ('branch:view', 'View Branches', 'branch'),
    ('branch:create', 'Create Branch', 'branch'),
    ('branch:update', 'Update Branch', 'branch'),

    -- Product module
    ('product:view', 'View Products', 'product'),
    ('product:create', 'Create Product', 'product'),
    ('product:update', 'Update Product', 'product'),

    -- Inventory module
    ('inventory:view', 'View Inventory', 'inventory'),
    ('inventory:update', 'Update Inventory', 'inventory'),

    -- Order module
    ('order:view', 'View Orders', 'order'),
    ('order:create', 'Create Order', 'order'),
    ('order:update', 'Update Order', 'order'),
    ('order:cancel', 'Cancel Order', 'order'),

    -- Customer module
    ('customer:view', 'View Customers', 'customer'),
    ('customer:create', 'Create Customer', 'customer'),
    ('customer:update', 'Update Customer', 'customer'),

    -- Report module
    ('report:branch', 'View Branch Reports', 'report'),
    ('report:company', 'View Company Reports', 'report');

-- Assign Permissions to Roles

-- SUPER_ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN';

-- BRANCH_MANAGER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'BRANCH_MANAGER'
  AND p.code IN (
    'product:view',
    'inventory:view', 'inventory:update',
    'order:view', 'order:create', 'order:update', 'order:cancel',
    'customer:view', 'customer:create', 'customer:update',
    'report:branch'
);

-- SALES_STAFF
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'SALES_STAFF'
  AND p.code IN (
    'product:view',
    'inventory:view',
    'order:view', 'order:create',
    'customer:view', 'customer:create'
);

-- WAREHOUSE_STAFF
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'WAREHOUSE_STAFF'
  AND p.code IN (
    'product:view',
    'inventory:view', 'inventory:update'
);

-- ACCOUNTANT
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN (
    'order:view',
    'customer:view',
    'report:branch', 'report:company'
);

-- Create default admin user
-- Password: admin123 (BCrypt encoded)
INSERT INTO users (username, email, password_hash, full_name)
VALUES ('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IkXCPMCqPNX3OPFNXaDdhmzK7zR0U.', 'System Admin');

-- Assign SUPER_ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'SUPER_ADMIN';

-- Create sample branches
INSERT INTO branches (code, name, address, phone) VALUES
    ('HN01', 'Hanoi - Cau Giay', '123 Cau Giay, Hanoi', '024-1234-5678'),
    ('HN02', 'Hanoi - Dong Da', '456 Dong Da, Hanoi', '024-2345-6789'),
    ('HCM01', 'HCM - District 1', '789 Le Loi, District 1, HCM', '028-3456-7890');

-- Assign admin to all branches
INSERT INTO user_branches (user_id, branch_id, is_primary)
SELECT u.id, b.id, (b.code = 'HN01')
FROM users u, branches b
WHERE u.username = 'admin';