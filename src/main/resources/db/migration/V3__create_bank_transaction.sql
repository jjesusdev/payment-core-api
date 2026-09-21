CREATE TABLE bank_transaction (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kind            VARCHAR(20)   NOT NULL,
    amount          NUMERIC(19,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    reference       VARCHAR(36)   NOT NULL UNIQUE,
    from_account_id BIGINT        REFERENCES account (id),
    to_account_id   BIGINT        REFERENCES account (id),
    executed_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_tx_accounts ON bank_transaction (from_account_id, to_account_id);