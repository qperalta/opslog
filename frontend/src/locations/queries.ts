import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Location, LocationRequest } from './types'

export const useLocations = () =>
  useQuery({ queryKey: ['locations'], queryFn: () => api<Location[]>('/api/locations') })

export function useSaveLocation() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: LocationRequest }) =>
      id === undefined
        ? api<Location>('/api/locations', { method: 'POST', body })
        : api<Location>(`/api/locations/${id}`, { method: 'PUT', body }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['locations'] }),
  })
}

export function useDeleteLocation() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api<void>(`/api/locations/${id}`, { method: 'DELETE' }),
    // check-ins show the location name, so refresh them too
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['locations'] })
      client.invalidateQueries({ queryKey: ['checkins'] })
    },
  })
}
