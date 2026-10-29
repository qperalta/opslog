import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, toQuery } from '../api/client'
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
