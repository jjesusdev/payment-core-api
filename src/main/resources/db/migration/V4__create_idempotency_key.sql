CREATE TABLE idempotency_key (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES app_user(id),
  key VARCHAR(64) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  response_body TEXT NOT NULL,
  status_code INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, key)
);

CREATE INDEX idx_idempotency_key_created_at ON idempotency_key (created_at);
