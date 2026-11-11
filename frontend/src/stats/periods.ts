import { addDays, formatDay } from '../checkin/dates'
import type { HistoryRow } from '../history/types'
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

export interface Bucket {
  label: string
  value: number
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

function monthKeys(from: string, to: string) {
  const keys: string[] = []
  const start = toDate(from)
  for (let i = 0; ; i++) {
    const key = ymd(new Date(start.getFullYear(), start.getMonth() + i, 1)).slice(0, 7)
    if (key > to.slice(0, 7)) return keys
    keys.push(key)
  }
}

/* week: Mon-Sun, month: each day, year: each month, all: each month or each year past two years */
function bucketPlan(key: Exclude<PeriodKey, 'today'>, range: ReportRange) {
  if (key === 'week' || key === 'month') {
    const start = range.from
    const days = key === 'week' ? 7 : lastDay(toDate(start).getFullYear(), toDate(start).getMonth())
    const dates = Array.from({ length: days }, (_, i) => addDays(start, i))
    const options: Intl.DateTimeFormatOptions = key === 'week' ? { weekday: 'short' } : { day: 'numeric' }
    return { keyOf: (date: string) => date, keys: dates.map((date) => ({ key: date, label: formatDay(date, options) })) }
  }
  const from = key === 'year' ? range.from : range.from.slice(0, 7) + '-01'
  const to = key === 'year' ? range.from.slice(0, 4) + '-12-31' : range.to
  const months = monthKeys(from, to)
  if (months.length > 24) {
    const years = Array.from(new Set(months.map((month) => month.slice(0, 4))))
    return { keyOf: (date: string) => date.slice(0, 4), keys: years.map((year) => ({ key: year, label: year })) }
  }
  const options: Intl.DateTimeFormatOptions = key === 'year' ? { month: 'short' } : { month: 'short', year: '2-digit' }
  return {
    keyOf: (date: string) => date.slice(0, 7),
    keys: months.map((month) => ({ key: month, label: formatDay(`${month}-01`, options) })),
  }
}

export function liftedBuckets(key: Exclude<PeriodKey, 'today'>, range: ReportRange, rows: TrainingReportRow[]): Bucket[] {
  const plan = bucketPlan(key, range)
  const sums = new Map<string, number>()
  for (const row of rows) {
    if (!inRange(row.date, range)) continue
    const bucket = plan.keyOf(row.date)
    sums.set(bucket, (sums.get(bucket) ?? 0) + row.volumeLb)
  }
  return plan.keys.map(({ key: bucket, label }) => ({ label, value: sums.get(bucket) ?? 0 }))
}

/* bodyweight and timed sets lift 0 lb, so they drop out */
export function liftedByExercise(sets: HistoryRow[]): Bucket[] {
  const sums = new Map<string, number>()
  for (const set of sets) {
    sums.set(set.exercise, (sums.get(set.exercise) ?? 0) + (set.reps ?? 0) * (set.loadLb ?? 0))
  }
  return [...sums].map(([label, value]) => ({ label, value })).filter((bucket) => bucket.value > 0)
}

export function workoutsPerWeek(rows: TrainingReportRow[], today: string, weeks = 8): Bucket[] {
  const thisMonday = mondayOf(today)
  return Array.from({ length: weeks }, (_, i) => {
    const from = addDays(thisMonday, -7 * (weeks - 1 - i))
    return {
      label: formatDay(from, { month: 'numeric', day: 'numeric' }),
      value: totals(rows, { from, to: addDays(from, 6) }).workouts,
    }
  })
}

/* weeks in a row with a workout; the current week joins once it has one */
export function weekStreak(rows: TrainingReportRow[], today: string) {
  const trained = new Set(rows.filter((row) => row.setsLogged > 0).map((row) => mondayOf(row.date)))
  let monday = mondayOf(today)
  if (!trained.has(monday)) monday = addDays(monday, -7)
  let streak = 0
  while (trained.has(monday)) {
    streak++
    monday = addDays(monday, -7)
  }
  return streak
}
