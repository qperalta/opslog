import {
  Alert, Box, Card, CardContent, Chip, LinearProgress, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
  TableRow, TextField, Typography,
} from '@mui/material'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { addDays, formatDay, todayLocal } from '../checkin/dates'
import { useBalanceReport, useTrainingReport } from './queries'
import type { BalanceReport, ReportRange, TrainingReport } from './types'

// same limits as the server
const DEFAULT_DAYS = 28
const MAX_DAYS = 366
const DAY_MS = 24 * 60 * 60 * 1000

const shortDay = (date: string) => formatDay(date, { month: 'short', day: 'numeric' })
const pounds = (value: number) => value.toLocaleString(undefined, { maximumFractionDigits: 0 })

function rangeError({ from, to }: ReportRange) {
  if (!from || !to) return 'Pick both dates'
  if (from > to) return 'Must be on or after From'
  if ((Date.parse(to) - Date.parse(from)) / DAY_MS >= MAX_DAYS) return `Pick ${MAX_DAYS} days or fewer`
  return null
}

export function DashboardSection() {
  const today = todayLocal()
  const [range, setRange] = useState<ReportRange>({ from: addDays(today, -(DEFAULT_DAYS - 1)), to: today })

  // checked here too so a bad range is never sent
  const error = rangeError(range)
  const training = useTrainingReport(range, !error)
  const balance = useBalanceReport(range, !error)

  return (
    <>
      <Stack direction="row" spacing={1.5} sx={{ mb: 2 }}>
        <TextField type="date" label="From" size="small" fullWidth value={range.from}
                   onChange={(event) => setRange({ ...range, from: event.target.value })}
                   slotProps={{ inputLabel: { shrink: true } }} error={!!error} />
        <TextField type="date" label="To" size="small" fullWidth value={range.to}
                   onChange={(event) => setRange({ ...range, to: event.target.value })}
                   slotProps={{ inputLabel: { shrink: true } }} error={!!error} helperText={error ?? undefined} />
      </Stack>

      {(training.isFetching || balance.isFetching) && <LinearProgress sx={{ mb: 1 }} />}
      {training.isError && <Alert severity="error" sx={{ mb: 2 }}>{training.error.message}</Alert>}
      {balance.isError && <Alert severity="error" sx={{ mb: 2 }}>{balance.error.message}</Alert>}

      {training.data && <TrainingReportCard report={training.data} />}
      {balance.data && <BalanceReportCard report={balance.data} />}
    </>
  )
}

function ReportCard({ title, generatedAt, from, to, children }: {
  title: string
  generatedAt: string
  from: string
  to: string
  children: ReactNode
}) {
  return (
    <Card variant="outlined" sx={{ mb: 3 }}>
      <CardContent sx={{ pb: 1 }}>
        <Typography variant="h6" component="h2">{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {shortDay(from)} – {shortDay(to)} · Generated {new Date(generatedAt).toLocaleString()}
        </Typography>
      </CardContent>
      {children}
    </Card>
  )
}

function TrainingReportCard({ report }: { report: TrainingReport }) {
  return (
    <ReportCard {...report}>
      <Stack direction="row" spacing={1} useFlexGap sx={{ px: 2, pb: 1.5, flexWrap: 'wrap' }}>
        <Chip size="small" label={`${report.sessions} ${report.sessions === 1 ? 'session' : 'sessions'}`} />
        <Chip size="small" label={`${report.setsLogged} sets`} />
        <Chip size="small" label={`${pounds(report.volumeLb)} lb volume`} />
      </Stack>
      {report.rows.length === 0 && <Alert severity="info" sx={{ mx: 2, mb: 2 }}>No sessions in this range.</Alert>}
      {report.rows.length > 0 && (
        <TableContainer>
          <Table size="small" aria-label={report.title}>
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell align="right">Sets</TableCell>
                <TableCell align="right">Clean</TableCell>
                <TableCell align="right">Volume (lb)</TableCell>
                <TableCell>Workout</TableCell>
                <TableCell align="right">Exercises</TableCell>
                <TableCell>Tier</TableCell>
                <TableCell>Location</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {report.rows.map((row) => (
                <TableRow key={row.date}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{shortDay(row.date)}</TableCell>
                  <TableCell align="right">{row.setsLogged}</TableCell>
                  <TableCell align="right">{row.cleanPercent == null ? '—' : `${row.cleanPercent}%`}</TableCell>
                  <TableCell align="right">{pounds(row.volumeLb)}</TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.workout}</TableCell>
                  <TableCell align="right">{row.exercisesDone}/{row.exercisesPlanned}</TableCell>
                  <TableCell>{row.tier}</TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.location ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </ReportCard>
  )
}

function ShareBar({ percent }: { percent: number }) {
  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', minWidth: 80 }}>
      <Box sx={{ flexGrow: 1 }}>
        <LinearProgress variant="determinate" value={percent} aria-hidden />
      </Box>
      <Typography variant="body2" sx={{ width: 36, textAlign: 'right' }}>{percent}%</Typography>
    </Stack>
  )
}

function BalanceReportCard({ report }: { report: BalanceReport }) {
  return (
    <ReportCard {...report}>
      <Box sx={{ px: 2, pb: 1 }}>
        <Typography variant="subtitle2" component="h3">Push vs. pull</Typography>
        <Typography variant="body2" color="text.secondary">
          {report.pushPullRatio == null
            ? 'No pull sets logged yet, so there is no ratio.'
            : `${report.pushPullRatio.toFixed(2)} push sets for every pull set`}
        </Typography>
      </Box>
      <TableContainer sx={{ mb: 2 }}>
        <Table size="small" aria-label="Push vs. pull">
          <TableHead>
            <TableRow>
              <TableCell>Direction</TableCell>
              <TableCell align="right">Sets</TableCell>
              <TableCell>Share</TableCell>
              <TableCell align="right">Sessions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.directions.map((row) => (
              <TableRow key={row.direction}>
                <TableCell>{row.direction}</TableCell>
                <TableCell align="right">{row.sets}</TableCell>
                <TableCell><ShareBar percent={row.sharePercent} /></TableCell>
                <TableCell align="right">{row.sessions}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box sx={{ px: 2, pb: 1 }}>
        <Typography variant="subtitle2" component="h3">By muscle</Typography>
        <Typography variant="body2" color="text.secondary">
          {report.totalSets} logged {report.totalSets === 1 ? 'set' : 'sets'}, counted by primary muscle
        </Typography>
      </Box>
      <TableContainer>
        <Table size="small" aria-label="By muscle">
          <TableHead>
            <TableRow>
              <TableCell>Muscle</TableCell>
              <TableCell align="right">Sets</TableCell>
              <TableCell>Share</TableCell>
              <TableCell align="right">Sessions</TableCell>
              <TableCell>Last trained</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {report.muscles.map((row) => (
              <TableRow key={row.muscle} sx={{ opacity: row.sets === 0 ? 0.6 : 1 }}>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.muscle}</TableCell>
                <TableCell align="right">{row.sets}</TableCell>
                <TableCell><ShareBar percent={row.sharePercent} /></TableCell>
                <TableCell align="right">{row.sessions}</TableCell>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.lastTrained ? shortDay(row.lastTrained) : '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </ReportCard>
  )
}
