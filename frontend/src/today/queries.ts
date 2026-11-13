import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ExerciseTimingBody, InsertExerciseBody, LoggedSet, LoggedSetBody, Session, SessionExercise } from './types'

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

// the server moves the later exercises down, so the whole session comes back
export function useInsertExercise() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ date, body }: { date: string, body: InsertExerciseBody }) =>
      api<Session>(`/api/sessions/${date}/exercises`, { method: 'POST', body }),
    onSuccess: (session) => {
      client.setQueryData(['sessions', session.date], session)
      // a new name is now one of the member's own exercises
      client.invalidateQueries({ queryKey: ['exercises'] })
    },
  })
}

// saved per exercise, so every session that has it picks the times up
export function useSetTiming() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ exerciseId, body }: { exerciseId: number, body: ExerciseTimingBody }) =>
      api<ExerciseTimingBody>(`/api/exercise-timings/${exerciseId}`, { method: 'PUT', body }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['sessions'] }),
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

export function useSetSkipped() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ date, position, skipped }: { date: string, position: number, skipped: boolean }) =>
      api<SessionExercise>(`/api/sessions/${date}/exercises/${position}/skipped`, { method: 'PUT', body: { skipped } }),
    onSuccess: (_, { date }) => client.invalidateQueries({ queryKey: ['sessions', date] }),
  })
}

export function useDeleteSet() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (target: SetTarget) => api<void>(`${setsPath(target)}/${target.setNumber}`, { method: 'DELETE' }),
    onSuccess: (_, target) => client.invalidateQueries({ queryKey: ['sessions', target.date] }),
  })
}
