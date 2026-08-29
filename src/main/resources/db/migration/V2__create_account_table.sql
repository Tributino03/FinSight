CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    account_type VARCHAR(30) NOT NULL,

    balance NUMERIC(19, 2) NOT NULL,

    user_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_accounts_balance
        CHECK (balance >= 0)
);