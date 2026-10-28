import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Session } from './types'

// the first call builds the session; later calls return the stored one
export const useSession = (date: string) =>
  useQuery({ queryKey: ['sessions', date], queryFn: () => api<Session>(`/api/sessions/${date}`) })

export function useRebuildSession() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (date: string) => api<Session>(`/api/sessions/${date}/rebuild`, { method: 'POST' }),
    onSuccess: (session) => client.setQueryData(['sessions', session.date], session),
  })
}
