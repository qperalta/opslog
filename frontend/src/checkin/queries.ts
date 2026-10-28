import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import type { CheckIn, CheckInRequest, Tier } from './types'

export const useTiers = () =>
  useQuery({ queryKey: ['tiers'], queryFn: () => api<Tier[]>('/api/tiers'), staleTime: Infinity })

// last 14 days, newest first from the server
export const useCheckIns = () =>
  useQuery({ queryKey: ['checkins'], queryFn: () => api<CheckIn[]>('/api/checkins') })

/* null when there is no check-in for the date yet (404) */
export const useCheckIn = (date: string) =>
  useQuery({
    queryKey: ['checkins', date],
    queryFn: () =>
      api<CheckIn>(`/api/checkins/${date}`).catch((error) => {
        if (error instanceof ApiError && error.status === 404) return null
        throw error
      }),
  })

export function useSaveCheckIn() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ date, body }: { date: string; body: CheckInRequest }) =>
      api<CheckIn>(`/api/checkins/${date}`, { method: 'PUT', body }),
    // a session waiting on the check-in can now be built
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['checkins'] })
      client.invalidateQueries({ queryKey: ['sessions'] })
    },
  })
}
