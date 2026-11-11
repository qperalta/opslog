import { addDays } from '../checkin/dates'
import type { ReportRange, TrainingReportRow } from './types'

export type PeriodKey = 'today' | 'week' | 'month' | 'year' | 'all'

export const PERIODS: { key: PeriodKey; label: string; versus: string | null }[] = [
  { key: 'today', label: 'Today', versus: 'yesterday' },
  { key: 'week', label: 'Week', versus: 'last week' },
  { key: 'month', label: 'Month', versus: 'last month' },
  { key: 'year', label: 'Year', versus: 'last year' },
  { key: 'all', label: 'All', versus: null },
]

export interface Totals {
  workouts: number
  sets: number
  lifted: number
}

// the report endpoints take at most 366 days per request
const MAX_DAYS = 366

export const compact = (value: number) =>
  value.toLocaleString(undefined, { notation: 'compact', maximumFractionDigits: 1 })

const toDate = (date: string) => {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day)
}
const ymd = (date: Date) => date.toLocaleDateString('en-CA')
const lastDay = (year: number, monthIndex: number) => new Date(year, monthIndex + 1, 0).getDate()
const inRange = (date: string, { from, to }: ReportRange) => date >= from && date <= to

export const mondayOf = (date: string) => addDays(date, -((toDate(date).getDay() + 6) % 7))

export function periodRange(key: PeriodKey, today: string, first: string | null): ReportRange {
  const now = toDate(today)
  switch (key) {
    case 'today': return { from: today, to: today }
    case 'week': return { from: mondayOf(today), to: today }
    case 'month': return { from: ymd(new Date(now.getFullYear(), now.getMonth(), 1)), to: today }
    case 'year': return { from: ymd(new Date(now.getFullYear(), 0, 1)), to: today }
    case 'all': return { from: first && first < today ? first : today, to: today }
  }
}

/* the same stretch of the period before, so a half-done week compares to a half-done week */
export function previousRange(key: PeriodKey, today: string): ReportRange | null {
  const now = toDate(today)
  const year = now.getFullYear()
  const month = now.getMonth()
  const day = now.getDate()
  switch (key) {
    case 'today': return { from: addDays(today, -1), to: addDays(today, -1) }
    case 'week': return { from: addDays(mondayOf(today), -7), to: addDays(today, -7) }
    case 'month': return {
      from: ymd(new Date(year, month - 1, 1)),
      to: ymd(new Date(year, month - 1, Math.min(day, lastDay(year, month - 1)))),
    }
    case 'year': return {
      from: ymd(new Date(year - 1, 0, 1)),
      to: ymd(new Date(year - 1, month, Math.min(day, lastDay(year - 1, month)))),
    }
    case 'all': return null
  }
}

export function chunks({ from, to }: ReportRange): ReportRange[] {
  const parts: ReportRange[] = []
  for (let start = from; start <= to; start = addDays(start, MAX_DAYS)) {
    const end = addDays(start, MAX_DAYS - 1)
    parts.push({ from: start, to: end < to ? end : to })
  }
  return parts
}

/* a session counts as a workout once it has a logged set */
export function totals(rows: TrainingReportRow[], range: ReportRange): Totals {
  return rows
    .filter((row) => row.setsLogged > 0 && inRange(row.date, range))
    .reduce((sum, row) => ({
      workouts: sum.workouts + 1,
      sets: sum.sets + row.setsLogged,
      lifted: sum.lifted + row.volumeLb,
    }), { workouts: 0, sets: 0, lifted: 0 })
}
