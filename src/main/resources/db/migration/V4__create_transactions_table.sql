CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,

    amount NUMERIC(19, 2) NOT NULL,

    description VARCHAR(255) NOT NULL,

    transaction_type VARCHAR(20) NOT NULL,

    payment_method VARCHAR(30) NOT NULL,

    transaction_status VARCHAR(20) NOT NULL,

    transaction_date TIMESTAMP NOT NULL,

    account_id BIGINT NOT NULL,

    category_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT chk_transactions_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('CREDIT', 'DEBIT')),

    CONSTRAINT chk_transactions_status
        CHECK (
            transaction_status IN (
                'PENDING',
                'COMPLETED',
                'CANCELLED',
                'FAILED'
            )
        )
);