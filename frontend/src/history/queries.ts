import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, toQuery } from '../api/client'
import type { Page } from '../api/types'
import type { HistoryRow, HistorySearch } from './types'

export const useHistory = (search: HistorySearch, enabled: boolean) =>
  useQuery({
    queryKey: ['history', search],
    queryFn: () => api<Page<HistoryRow>>(`/api/history${toQuery(search)}`),
    placeholderData: keepPreviousData,
    enabled,
  })
