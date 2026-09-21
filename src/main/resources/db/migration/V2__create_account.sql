CREATE TABLE account (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_no VARCHAR(20)  NOT NULL UNIQUE,
    owner_id   BIGINT       NOT NULL REFERENCES app_user (id),
    currency   VARCHAR(3)   NOT NULL DEFAULT 'CLP',
    balance    NUMERIC(19,2) NOT NULL DEFAULT 0,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_owner ON account (owner_id);