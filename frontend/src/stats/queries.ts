import { keepPreviousData, useQueries, useQuery } from '@tanstack/react-query'
import { api, toQuery } from '../api/client'
import type { Page } from '../api/types'
import type { HistoryRow } from '../history/types'
import { chunks } from './periods'
import type { BalanceReport, ReportRange, TrainingReport } from './types'

export const useTrainingReport = (range: ReportRange, enabled: boolean) =>
  useQuery({
    queryKey: ['reports', 'training', range],
    queryFn: () => api<TrainingReport>(`/api/reports/training${toQuery(range)}`),
    placeholderData: keepPreviousData,
    enabled,
  })

export const useBalanceReport = (range: ReportRange, enabled: boolean) =>
  useQuery({
    queryKey: ['reports', 'balance', range],
    queryFn: () => api<BalanceReport>(`/api/reports/balance${toQuery(range)}`),
    placeholderData: keepPreviousData,
    enabled,
  })

/* history is newest first, so the last one-row page is the oldest logged set */
export const useFirstLogDate = () =>
  useQuery({
    queryKey: ['history', 'first'],
    queryFn: async () => {
      const newest = await api<Page<HistoryRow>>(`/api/history${toQuery({ size: 1 })}`)
      if (newest.totalElements === 0) return null
      const oldest = await api<Page<HistoryRow>>(`/api/history${toQuery({ page: newest.totalElements - 1, size: 1 })}`)
      return oldest.items[0]?.date ?? null
    },
  })

/* one request per 366 days, merged back into one list */
export const useTrainingRows = (range: ReportRange | null) =>
  useQueries({
    queries: (range ? chunks(range) : []).map((part) => ({
      queryKey: ['reports', 'training', part],
      queryFn: () => api<TrainingReport>(`/api/reports/training${toQuery(part)}`),
    })),
    combine: (results) => ({
      rows: results.flatMap((result) => result.data?.rows ?? []),
      pending: !range || results.some((result) => result.isPending),
      error: results.find((result) => result.error)?.error ?? null,
    }),
  })
