/* V16: the assistant's name from the MCP handshake (e.g. "Claude"), saved on the token it used */
ALTER TABLE mcp_tokens ADD COLUMN client_name VARCHAR(60);
