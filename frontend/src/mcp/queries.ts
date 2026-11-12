import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { IssuedMcpToken, McpToken, McpTokenRequest } from './types'

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
    onSuccess: () => client.invalidateQueries({ queryKey: ['mcp-tokens']})
  })
}