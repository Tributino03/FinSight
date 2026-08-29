CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    category_type VARCHAR(20) NOT NULL,

    user_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_categories_type
        CHECK (category_type IN ('INCOME', 'EXPENSE'))
);