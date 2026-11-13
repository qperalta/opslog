import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Notifications } from './types'

export const useNotifications = () =>
  useQuery({ queryKey: ['notifications'], queryFn: () => api<Notifications>('/api/notifications') })

export function useMarkNotificationsRead() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>('/api/notifications/read', { method: 'POST' }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }),
  })
}
