ALTER TABLE accounts
ADD CONSTRAINT chk_accounts_account_type
CHECK (account_type IN (
    'CHECKING',
    'SAVINGS',
    'WALLET',
    'INVESTMENT'
));

ALTER TABLE transactions
ADD CONSTRAINT chk_transactions_payment_method
CHECK (payment_method IN (
    'PIX',
    'CREDIT_CARD',
    'DEBIT_CARD',
    'BANK_TRANSFER',
    'CASH'
));