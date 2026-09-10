-- Accounts depend on users
ALTER TABLE accounts
DROP CONSTRAINT fk_accounts_user;

ALTER TABLE accounts
ADD CONSTRAINT fk_accounts_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;


-- Categories depend on users
ALTER TABLE categories
DROP CONSTRAINT fk_categories_user;

ALTER TABLE categories
ADD CONSTRAINT fk_categories_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;


-- Transactions depend on accounts
ALTER TABLE transactions
DROP CONSTRAINT fk_transactions_account;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_account
FOREIGN KEY (account_id)
REFERENCES accounts(id)
ON DELETE CASCADE;


-- Transactions depend on categories
ALTER TABLE transactions
DROP CONSTRAINT fk_transactions_category;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_category
FOREIGN KEY (category_id)
REFERENCES categories(id)
ON DELETE CASCADE;