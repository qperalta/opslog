import { Alert, Grid, LinearProgress, ToggleButton, ToggleButtonGroup } from '@mui/material'
import { useState } from 'react'
import { todayLocal } from '../checkin/dates'
import { Bars, ChartCard, EmptyChart, SplitDonut } from './charts'
import {
  PERIODS, liftedBuckets, liftedByExercise, periodRange, previousRange, totals, weekStreak, workoutsPerWeek,
} from './periods'
import type { PeriodKey } from './periods'
import { useDaySets, useDirectionSplit, useFirstLogDate, useTrainingRows } from './queries'
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
  const split = useDirectionSplit(first.isSuccess ? range : null)
  const daySets = useDaySets(today, period === 'today')

  const versus = PERIODS.find((item) => item.key === period)?.versus ?? null
  const lifted = period === 'today'
    ? liftedByExercise(daySets.data ?? [])
    : liftedBuckets(period, range, training.rows)
  const weeks = workoutsPerWeek(training.rows, today)
  const streak = weekStreak(training.rows, today)
  const error = first.error ?? training.error ?? split.error ?? daySets.error

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
        <>
          <StatTiles current={totals(training.rows, range)} previous={previous && totals(training.rows, previous)}
                     versus={versus} />

          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid size={12}>
              <ChartCard title="Weight lifted" subtitle={period === 'today' ? 'By exercise, in pounds' : 'In pounds'}>
                {lifted.some((bucket) => bucket.value > 0)
                  ? <Bars data={lifted} unit="lb" horizontal={period === 'today'} />
                  : <EmptyChart text={period === 'today' ? 'Nothing lifted yet today' : 'Nothing lifted in this period'} />}
              </ChartCard>
            </Grid>
            <Grid size={6}>
              <ChartCard title="Workouts per week" subtitle={`${streak}-week streak`}>
                <Bars data={weeks} unit="workouts" height={140} />
              </ChartCard>
            </Grid>
            <Grid size={6}>
              <ChartCard title="Push vs. pull" subtitle="Sets in this period">
                {split.pending && <LinearProgress />}
                {!split.pending && split.total > 0 && <SplitDonut slices={split.slices} total={split.total} />}
                {!split.pending && split.total === 0 && <EmptyChart text="No sets yet" />}
              </ChartCard>
            </Grid>
          </Grid>
        </>
      )}

      <ReportsSection />
    </>
  )
}
