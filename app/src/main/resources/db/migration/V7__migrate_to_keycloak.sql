-- =============================================
-- V7: Migrate to Keycloak Authentication
-- =============================================
-- Keycloak sẽ quản lý: users, passwords, roles, permissions
-- App DB sẽ giữ: user profile, user_branches (business logic)

-- 1. Thêm keycloak_id vào users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_id VARCHAR(50) UNIQUE;

-- 2. Xóa password_hash (Keycloak quản lý password)
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;

-- 3. Xóa các bảng auth (Keycloak quản lý roles/permissions)
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;

-- 4. Tạo index cho keycloak_id (query performance)
CREATE INDEX IF NOT EXISTS idx_users_keycloak_id ON users(keycloak_id);

-- 5. Update users table - cho phép tạo user mới từ Keycloak sync
-- (username và email có thể được update từ Keycloak)
ALTER TABLE users ALTER COLUMN username DROP NOT NULL;
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- 6. Thêm column để track sync time
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

-- 7. Xóa admin user cũ (sẽ tạo lại trong Keycloak)
DELETE FROM user_branches WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';

-- =============================================
-- Schema sau migration:
-- =============================================
-- users:
--   id, keycloak_id, username, email, full_name,
--   is_active, created_at, updated_at, last_synced_at
--
-- user_branches: (giữ nguyên - business logic)
--   user_id, branch_id, is_primary
-- =============================================
