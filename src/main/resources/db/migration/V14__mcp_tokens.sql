CREATE TABLE mcp_tokens(
    id                              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                            VARCHAR(60)     NOT NULL,
    token_hash                      VARCHAR(64)     NOT NULL,
    scope                           VARCHAR(10)     NOT NULL,
    created_at                      timestamptz     NOT NULL DEFAULT now(),
    last_used_at                    timestamptz,
    revoked_at                      timestamptz,

    CONSTRAINT uq_mcp_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_mcp_tokens_scope CHECK (scope IN('READ', 'READ_WRITE'))
);

CREATE INDEX ix_mcp_tokens_user ON mcp_tokens (user_id);