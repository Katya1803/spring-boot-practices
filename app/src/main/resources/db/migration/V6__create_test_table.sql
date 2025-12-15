-- =============================================
-- TEST TABLE: For verifying setup
-- =============================================

CREATE TABLE test_items (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_items (name, description) VALUES
    ('Item 1', 'First test item'),
    ('Item 2', 'Second test item'),
    ('Item 3', 'Third test item');
