import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import LockIcon from '@mui/icons-material/Lock'
import {
  Alert, Box, Button, Card, CardActionArea, CardContent, Chip, IconButton, LinearProgress, Stack, Typography,
} from '@mui/material'
import { useState } from 'react'
import { addDays, formatDay, todayLocal } from '../checkin/dates'
import { labelOf } from '../components/format'
import { PageHeader } from '../components/PageHeader'
import { DayDialog } from './DayDialog'
import { ProgramForm } from './ProgramForm'
import { useProgram, usePrograms, useWeek } from './queries'
import type { Actor, PlanDay, Workout } from './types'

const SET_BY: Record<Actor, string> = { USER: 'Set by you', RULES: 'Planned by the rules engine', AGENT: 'Set by your AI coach' }

export function PlannerPage() {
  const today = todayLocal()
  const [weekOf, setWeekOf] = useState(today)
  const [changing, setChanging] = useState(false)
  const [editing, setEditing] = useState<PlanDay | null>(null)
  const program = useProgram()
  const programs = usePrograms()
  const week = useWeek(weekOf, !!program.data)

  const template = programs.data?.find((item) => item.id === program.data?.template.id)
  const workoutsById = new Map<number, Workout>(programs.data?.flatMap((item) => item.workouts).map((w) => [w.id, w]) ?? [])
  const thisWeek = week.data && today >= week.data.start && today <= week.data.end

  return (
    <>
      <PageHeader title="Planner" />

      {program.isPending && <LinearProgress />}
      {program.isError && <Alert severity="error">{program.error.message}</Alert>}

      {program.isSuccess && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            {!program.data || changing ? (
              <>
                {!program.data && (
                  <Typography color="text.secondary" sx={{ mb: 2 }}>
                    Pick a Kaimana program to start. The rules engine fills in your week from it.
                  </Typography>
                )}
                <ProgramForm current={program.data} onDone={program.data ? () => setChanging(false) : undefined} />
              </>
            ) : (
              <Stack spacing={1}>
                <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="h6" component="h2">{program.data.template.name}</Typography>
                  <Button size="small" onClick={() => setChanging(true)}>Change</Button>
                </Stack>
                {template && <Typography variant="body2" color="text.secondary">{template.description}</Typography>}
                <Stack direction="row" spacing={0.5} sx={{ flexWrap: 'wrap' }}>
                  {template?.trainingDays.map((day) => (
                    <Chip key={day} size="small" label={labelOf(day).slice(0, 3)} />
                  ))}
                </Stack>
                <Typography variant="body2">Usual location: {program.data.location?.name ?? 'None'}</Typography>
              </Stack>
            )}
          </CardContent>
        </Card>
      )}

      {program.data && (
        <>
          <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
            <IconButton aria-label="Previous week" onClick={() => setWeekOf(addDays(weekOf, -7))}>
              <ChevronLeftIcon />
            </IconButton>
            <Box sx={{ textAlign: 'center' }}>
              <Typography sx={{ fontWeight: 600 }}>
                {week.data
                  ? `${formatDay(week.data.start, { month: 'short', day: 'numeric' })} – ${formatDay(week.data.end, { month: 'short', day: 'numeric' })}`
                  : ' '}
              </Typography>
              {week.data && !thisWeek && (
                <Button size="small" onClick={() => setWeekOf(today)}>Back to this week</Button>
              )}
            </Box>
            <IconButton aria-label="Next week" onClick={() => setWeekOf(addDays(weekOf, 7))}>
              <ChevronRightIcon />
            </IconButton>
          </Stack>

          {week.isFetching && <LinearProgress sx={{ mb: 1 }} />}
          {week.isError && <Alert severity="error">{week.error.message}</Alert>}

          <Stack spacing={1}>
            {week.data?.days.map((day) => {
              const workout = day.workout ? workoutsById.get(day.workout.id) : undefined
              const past = day.date < today
              return (
                <Card key={day.date} variant="outlined" sx={{ opacity: past ? 0.6 : 1, borderColor: day.date === today ? 'primary.main' : undefined }}>
                  <CardActionArea disabled={past} onClick={() => setEditing(day)}>
                    <CardContent sx={{ py: 1.5 }}>
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
                        <Box sx={{ width: 44, flexShrink: 0, textAlign: 'center' }}>
                          <Typography variant="caption" color="text.secondary">{formatDay(day.date, { weekday: 'short' })}</Typography>
                          <Typography variant="h6" sx={{ lineHeight: 1.1 }}>{formatDay(day.date, { day: 'numeric' })}</Typography>
                        </Box>
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                            <Typography sx={{ fontWeight: 600 }} color={day.workout ? 'text.primary' : 'text.secondary'}>
                              {day.workout?.name ?? 'Rest'}
                            </Typography>
                            {day.date === today && <Chip size="small" color="primary" label="Today" />}
                            {day.mode !== 'AUTO' && <Chip size="small" variant="outlined" label={day.mode === 'REST' ? 'Rest day' : 'Training day'} />}
                          </Stack>
                          {workout && (
                            <Typography variant="body2" color="text.secondary" noWrap>
                              {workout.exercises.length} exercises · {workout.exercises.map((item) => item.exercise.name).join(', ')}
                            </Typography>
                          )}
                          <Typography variant="caption" color="text.secondary">
                            {day.location?.name ?? program.data?.location?.name ?? 'No location'} · {SET_BY[day.updatedBy]}
                          </Typography>
                        </Box>
                        {day.locked && <LockIcon fontSize="small" color="action" aria-label="Locked" />}
                      </Stack>
                    </CardContent>
                  </CardActionArea>
                </Card>
              )
            })}
          </Stack>
        </>
      )}

      {editing && (
        <DayDialog
          key={editing.date}
          day={editing}
          workouts={template?.workouts ?? []}
          defaultLocation={program.data?.location?.name ?? null}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}
