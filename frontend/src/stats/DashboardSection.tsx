import { Alert, LinearProgress, ToggleButton, ToggleButtonGroup } from '@mui/material'
import { useState } from 'react'
import { todayLocal } from '../checkin/dates'
import { PERIODS, periodRange, previousRange, totals } from './periods'
import type { PeriodKey } from './periods'
import { useFirstLogDate, useTrainingRows } from './queries'
import { ReportsSection } from './ReportsSection'
import { StatTiles } from './StatTiles'

export function DashboardSection() {
  const today = todayLocal()
  const [period, setPeriod] = useState<PeriodKey>('week')
  const first = useFirstLogDate()

  // every session since the first logged set, and back to last January 1 so the year has something to compare to
  const lastJanuary = `${Number(today.slice(0, 4)) - 1}-01-01`
  const firstDate = first.data ?? null
  const span = first.isSuccess ? { from: firstDate && firstDate < lastJanuary ? firstDate : lastJanuary, to: today } : null
  const training = useTrainingRows(span)

  const range = periodRange(period, today, firstDate)
  const previous = previousRange(period, today)
  const versus = PERIODS.find((item) => item.key === period)?.versus ?? null
  const error = first.error ?? training.error

  return (
    <>
      <ToggleButtonGroup exclusive fullWidth size="small" value={period} aria-label="Period" sx={{ mb: 2 }}
                         onChange={(_, value: PeriodKey | null) => value && setPeriod(value)}>
        {PERIODS.map((item) => (
          <ToggleButton key={item.key} value={item.key}>{item.label}</ToggleButton>
        ))}
      </ToggleButtonGroup>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error.message}</Alert>}
      {training.pending && !error && <LinearProgress sx={{ mb: 2 }} />}

      {!training.pending && (
        <StatTiles current={totals(training.rows, range)} previous={previous && totals(training.rows, previous)}
                   versus={versus} />
      )}

      <ReportsSection />
    </>
  )
}
