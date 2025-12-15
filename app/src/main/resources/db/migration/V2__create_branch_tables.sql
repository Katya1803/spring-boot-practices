-- =============================================
-- BRANCH MODULE: branches, user_branches
-- =============================================

-- Branches table
CREATE TABLE branches (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(255),
    phone           VARCHAR(20),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User-Branch mapping (Many-to-Many)
-- A user can belong to multiple branches
-- is_primary indicates the main branch for the user
CREATE TABLE user_branches (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    branch_id       BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, branch_id)
);

CREATE INDEX idx_user_branches_branch ON user_branches(branch_id);