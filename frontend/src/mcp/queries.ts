import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AiCoach, IssuedMcpToken, McpToken, McpTokenRequest } from './types'

// read on every visit to the Planner, so a fresh connection shows up without a reload
export const useAiCoach = () =>
  useQuery({ queryKey: ['ai-coach'], queryFn: () => api<AiCoach>('/api/ai-coach') })

export const useMcpTokens = () =>
  useQuery({ queryKey: ['mcp-tokens'], queryFn: () => api<McpToken[]>('/api/mcp-tokens') })

export const useIssueMcpToken = () => {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: McpTokenRequest) =>
      api<IssuedMcpToken>('/api/mcp-tokens', {
        method: 'POST',
        body
      }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['mcp-tokens'] })
  })
}

export const useRevokeMcpToken = () => {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api<McpToken>(`/api/mcp-tokens/${id}/revoke`, { method: 'POST' }),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['mcp-tokens'] })
      // a revoked token switches the coach off right away
      client.invalidateQueries({ queryKey: ['ai-coach'] })
    }
  })
}