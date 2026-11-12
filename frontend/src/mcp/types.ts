// mirrors MCPTokenResponse and IssuedMcpTokenResponse
export type McpScope = 'READ' | 'READ_WRITE'

export interface McpToken {
  id: number
  name: string
  scope: McpScope
  active: boolean
  createdAt: string
  lastUsedAt: string | null
  revokedAt: string | null
}

export interface IssuedMcpToken {
  token: McpToken
  secret: string
}

export interface McpTokenRequest {
  name: string
  scope: McpScope
}