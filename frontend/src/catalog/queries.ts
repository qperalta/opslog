import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, toQuery } from '../api/client'
import type { Lookup, Page } from '../api/types'
import type { Equipment, Exercise, ExerciseRequest, ExerciseSearch } from './types'

// lookups rarely change, so they are fetched once per session
const LOOKUP = { staleTime: Infinity }

export const useMuscleGroups = () =>
  useQuery({ queryKey: ['muscle-groups'], queryFn: () => api<Lookup[]>('/api/catalog/muscle-groups'), ...LOOKUP })

export const useMovementPatterns = () =>
  useQuery({ queryKey: ['movement-patterns'], queryFn: () => api<Lookup[]>('/api/catalog/movement-patterns'), ...LOOKUP })

export const useEquipment = () =>
  useQuery({ queryKey: ['equipment'], queryFn: () => api<Equipment[]>('/api/catalog/equipment'), ...LOOKUP })

export const useExerciseKinds = () =>
  useQuery({ queryKey: ['exercise-kinds'], queryFn: () => api<string[]>('/api/catalog/exercise-kinds'), ...LOOKUP })

export const useExercises = (search: ExerciseSearch) =>
  useQuery({
    queryKey: ['exercises', search],
    queryFn: () => api<Page<Exercise>>(`/api/catalog/exercises${toQuery(search)}`),
    placeholderData: keepPreviousData,
  })

export function useSaveExercise() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: ExerciseRequest }) =>
      id === undefined
        ? api<Exercise>('/api/catalog/exercises', { method: 'POST', body })
        : api<Exercise>(`/api/catalog/exercises/${id}`, { method: 'PUT', body }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['exercises'] }),
  })
}

export function useDeleteExercise() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/catalog/exercises/${id}`, { method: 'DELETE' }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['exercises'] }),
  })
}
