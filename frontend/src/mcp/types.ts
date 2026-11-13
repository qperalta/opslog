// mirrors MCPTokenResponse and IssuedMcpTokenResponse
export type McpScope = 'READ' | 'READ_WRITE'

export interface McpToken {
  id: number
  name: string
  scope: McpScope
  active: boolean
  createdAt: string
  lastUsedAt: string | null
  clientName: string | null
  revokedAt: string | null
}

export interface IssuedMcpToken {
  token: McpToken
  secret: string
}

// mirrors AiCoachResponse and AiActivityResponse
export type McpOutcome = 'OK' | 'DENIED' | 'FAILED'

export interface AiActivity {
  tool: string
  outcome: McpOutcome
  detail: string | null
  at: string
}

export interface AiCoach {
  active: boolean
  clientName: string | null
  lastActiveAt: string | null
  summary: string | null
  summaryAt: string | null
  recent: AiActivity[]
}

export interface McpTokenRequest {
  name: string
  scope: McpScope
}