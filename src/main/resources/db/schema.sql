-- Uygulama her açılışta bu betiği çalıştırır; IF NOT EXISTS sayesinde tekrar çalıştırmak güvenlidir.

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id      BIGSERIAL   PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name    VARCHAR(50) NOT NULL,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name),
    -- expenses tablosundaki bileşik yabancı anahtar için gerekli
    CONSTRAINT uq_categories_id_user UNIQUE (id, user_id)
);

CREATE TABLE IF NOT EXISTS expenses (
    id            BIGSERIAL     PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id   BIGINT        NOT NULL,
    expense_date  DATE          NOT NULL,
    description   VARCHAR(255)  NOT NULL,
    amount        NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency      VARCHAR(3)    NOT NULL CHECK (currency IN ('TRY', 'USD', 'EUR')),
    exchange_rate NUMERIC(12, 6) NOT NULL CHECK (exchange_rate > 0),
    amount_try    NUMERIC(14, 2) NOT NULL CHECK (amount_try > 0),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Harcamanın kategorisi aynı kullanıcıya ait olmak zorunda
    CONSTRAINT fk_expenses_category_same_user
        FOREIGN KEY (category_id, user_id) REFERENCES categories (id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories (user_id);
CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses (user_id);
