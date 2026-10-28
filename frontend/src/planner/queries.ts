import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../api/client'
import type { PlanDay, PlanDayRequest, ProgramTemplate, UserProgram, UserProgramRequest, Week } from './types'

export const usePrograms = () =>
  useQuery({ queryKey: ['programs'], queryFn: () => api<ProgramTemplate[]>('/api/programs'), staleTime: Infinity })

/* null until the user picks a program (404) */
export const useProgram = () =>
  useQuery({
    queryKey: ['program'],
    queryFn: () =>
      api<UserProgram>('/api/program').catch((error) => {
        if (error instanceof ApiError && error.status === 404) return null
        throw error
      }),
  })

// any date in the week; the server snaps to Monday
export const useWeek = (date: string, enabled: boolean) =>
  useQuery({
    queryKey: ['plan', date],
    queryFn: () => api<Week>(`/api/plan?week=${date}`),
    enabled,
    placeholderData: keepPreviousData,
  })

export function useChooseProgram() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: UserProgramRequest) => api<UserProgram>('/api/program', { method: 'PUT', body }),
    // switching re-plans the days ahead
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['program'] })
      client.invalidateQueries({ queryKey: ['plan'] })
    },
  })
}

export function useSaveDay() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ date, body }: { date: string; body: PlanDayRequest }) =>
      api<PlanDay>(`/api/plan/${date}`, { method: 'PUT', body }),
    // an edit can shift the rotation of later weeks too
    onSuccess: () => client.invalidateQueries({ queryKey: ['plan'] }),
  })
}
