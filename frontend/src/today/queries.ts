import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { LoggedSet, LoggedSetBody, Session } from './types'

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

interface SetTarget {
  date: string
  position: number
  setNumber?: number
}

const setsPath = ({ date, position }: SetTarget) => `/api/sessions/${date}/exercises/${position}/sets`

// no setNumber = a new set
export function useSaveSet() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ target, body }: { target: SetTarget, body: LoggedSetBody }) => target.setNumber == null
      ? api<LoggedSet>(setsPath(target), { method: 'POST', body })
      : api<LoggedSet>(`${setsPath(target)}/${target.setNumber}`, { method: 'PUT', body }),
    onSuccess: (_, { target }) => client.invalidateQueries({ queryKey: ['sessions', target.date] }),
  })
}

export function useDeleteSet() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (target: SetTarget) => api<void>(`${setsPath(target)}/${target.setNumber}`, { method: 'DELETE' }),
    onSuccess: (_, target) => client.invalidateQueries({ queryKey: ['sessions', target.date] }),
  })
}
