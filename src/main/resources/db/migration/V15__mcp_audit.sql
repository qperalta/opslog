/* V15: One row per MCP tool call, so every agent read and write can be traced to its token */
CREATE TABLE mcp_audit(
      id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
      user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
      token_id            BIGINT       NOT NULL REFERENCES mcp_tokens (id) ON DELETE CASCADE,
      tool                VARCHAR(40)  NOT NULL,
      outcome             VARCHAR(10)  NOT NULL,
      detail              VARCHAR(200),
      created_at          timestamptz  NOT NULL DEFAULT now(),

      CONSTRAINT ck_mcp_audit_outcome CHECK (outcome IN ('OK', 'DENIED', 'FAILED'))
);

CREATE INDEX ix_mcp_audit_user_created ON mcp_audit (user_id, created_at DESC);
CREATE INDEX ix_mcp_audit_token ON mcp_audit (token_id);
