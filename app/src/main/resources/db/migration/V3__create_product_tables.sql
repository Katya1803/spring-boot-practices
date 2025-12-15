-- =============================================
-- PRODUCT MODULE: categories, products, inventory
-- =============================================

-- Categories table (self-referencing for hierarchy)
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    parent_id       BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_parent ON categories(parent_id);

-- Products table
CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    sku             VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    category_id     BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    base_price      DECIMAL(15, 2) NOT NULL,
    unit            VARCHAR(20) NOT NULL DEFAULT 'piece',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active);

-- Branch Inventory (stock per branch)
CREATE TABLE branch_inventory (
    branch_id       BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity        INT NOT NULL DEFAULT 0,
    min_stock       INT NOT NULL DEFAULT 10,
    last_updated    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id, product_id),
    CONSTRAINT chk_quantity_positive CHECK (quantity >= 0)
);