import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  Accordion, AccordionDetails, AccordionSummary, Alert, AlertTitle, Box, Button, Card, CardContent, Chip, LinearProgress,
  Stack, Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router'
import { ApiError } from '../api/client'
import { CheckInForm } from '../checkin/CheckInForm'
import { CheckInHistory } from '../checkin/CheckInHistory'
import { formatDay, todayLocal } from '../checkin/dates'
import { useCheckIn } from '../checkin/queries'
import { TierRules } from '../checkin/TierRules'
import { tierColor } from '../checkin/tierColor'
import { HeaderStats, type HeaderStat } from '../components/HeaderStat'
import { useNotify } from '../components/NotifyContext'
import { PageHeader } from '../components/PageHeader'
import { useRebuildSession, useSession } from './queries'
import { RestTimer } from './RestTimer'
import { clock, describeSet, isStrength, restSeconds } from './setFormat'
import type { Session, SessionExercise } from './types'
import { useStoredTimer } from './useStoredTimer'
import { useWakeLock } from './useWakeLock'
import { useWorkoutMode } from './useWorkoutMode'
import { WorkoutCard } from './WorkoutCard'

const BUILT_BY = { USER: 'Built by you', RULES: 'Built by the rules engine', AGENT: 'Written by your AI coach' }

const subtitleFor = (session: Session | undefined, noProgram: boolean)=> {
  if(noProgram) return 'No program picked yet'
  if(session?.status === 'REST') return 'Rest day'
  if(session?.status === 'NEEDS_CHECKIN') return `${session.workout?.name ?? 'Training' } today · check in to build it`
  if(session?.status === 'READY') {
    const name = session.workout?.name ?? 'Session'
    return session.source ? `${name} · ${BUILT_BY[session.source]}` : name
  }
  return undefined
}

const sessionStat = (session: Session): HeaderStat[] => {
  const logged = session.exercises.reduce((sum, item) => sum + item.logged.length, 0)
  const planned = session.exercises.reduce((sum, item) => sum + (item.skipped ? 0 : item.sets), 0)
  const tier = session.tier && (
    <>
      <Box
        component="span"
        sx={{
          display: 'inline-block',
          width: 8,
          height: 8,
          borderRadius: '50%',
          mr: 1,
          verticalAlign: 'middle',
          bgcolor: `${tierColor(session.tierKind ?? '')}.main`
        }} />
      {session.tier.name}
    </>
  )
  return [
    { label: 'Tier', value: tier ?? '-' },
    { label: 'Exercise', value: session.exercises.length },
    { label: 'Sets', value: `${logged} / ${planned}` }
  ]
}

export function TodayPage() {
  const today = todayLocal()
  const session = useSession(today)
  const checkIn = useCheckIn(today)
  const rebuild = useRebuildSession()
  const notify = useNotify()
  const workout = useWorkoutMode(today)
  // during a workout the page shows only the exercises and Finish workout
  const inWorkout = workout.active && session.data?.status === 'READY'

  const noProgram = session.error instanceof ApiError && session.error.status === 404

  // the session is kept as built; a later check-in only shows up after a rebuild
  const outOfDate = session.data?.status === 'READY' && !!checkIn.data && (
    checkIn.data.tier.id !== session.data.tier?.id
    || (!!checkIn.data.location && checkIn.data.location.id !== session.data.location?.id)
  )

  const onRebuild = () =>
    rebuild.mutate(today, {
      onSuccess: () => notify('Session rebuilt'),
      onError: (error) => notify(error.message, 'error'),
    })

  if(inWorkout && session.data) {
    return <ReadySession date={today} session={session.data} workout={workout} outOfDate={false} rebuilding={false} onRebuild={onRebuild} />
  }

  return (
    <>
      <PageHeader
        eyebrow={formatDay(today, { weekday: 'long', month: 'long', day: 'numeric' })}
        title="Today"
        subtitle={subtitleFor(session.data, noProgram)}
      >
        {session.data?.status === 'READY' && <HeaderStats stats={sessionStat(session.data)} />}
      </PageHeader>

      {/* the check-in card shows until today's check-in exists */}
      {checkIn.isError && <Alert severity="error" sx={{ mb: 2 }}>{checkIn.error.message}</Alert>}
      {checkIn.isSuccess && checkIn.data === null && (
        <Card sx={{ mb: 2 }}>
          <CardContent>
            <Typography variant="h6" component="h2" sx={{ mb: 2 }}>Morning check-in</Typography>
            <CheckInForm key={today} date={today} existing={null} />
            <Accordion disableGutters elevation={0} sx={{ mt: 2, '&::before': { display: 'none' } }}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 0 }}>
                <Typography variant="body2" color="text.secondary">How the tier is picked</Typography>
              </AccordionSummary>
              <AccordionDetails sx={{ px: 0 }}>
                <TierRules />
              </AccordionDetails>
            </Accordion>
          </CardContent>
        </Card>
      )}

      {session.isPending && <LinearProgress />}
      {session.isError && !noProgram && <Alert severity="error">{session.error.message}</Alert>}

      {noProgram && (
        <Alert severity="info" action={<Button component={RouterLink} to="/planner" size="small" sx={{ whiteSpace: 'nowrap' }}>Planner</Button>}>
          Pick a program in the Planner to get a session.
        </Alert>
      )}

      {session.data?.status === 'REST' && (
        <Alert severity="info" action={<Button component={RouterLink} to="/planner" size="small" sx={{ whiteSpace: 'nowrap' }}>Planner</Button>}>
          <AlertTitle>Rest day</AlertTitle>
          Nothing is planned today. Recover and come back tomorrow.
        </Alert>
      )}

      {session.data?.status === 'NEEDS_CHECKIN' && (
        <Alert severity="warning">
          <AlertTitle>{session.data.workout?.name ?? 'Training'} today</AlertTitle>
          Check in above first. Your sleep and soreness set the tier, and your location sets the equipment.
        </Alert>
      )}

      {session.data?.status === 'READY' && (
        <ReadySession date={today} session={session.data} workout={workout} outOfDate={outOfDate} rebuilding={rebuild.isPending} onRebuild={onRebuild} />
      )}

      <Box sx={{ mt: 3 }}>
        <CheckInHistory />
      </Box>
    </>
  )
}

interface ReadyProps {
  date: string
  session: Session
  workout: ReturnType<typeof useWorkoutMode>
  outOfDate: boolean
  rebuilding: boolean
  onRebuild: () => void
}

function ReadySession({ date, session, workout, outOfDate, rebuilding, onRebuild }: ReadyProps) {
  const [restEndsAt, setRestEndsAt] = useStoredTimer('opslog.rest', date)
  useWakeLock(workout.active)
  const notify = useNotify()

  // rebuilding would delete the log, so the server refuses it
  const hasLogs = session.exercises.some((item) => item.logged.length > 0)

  return (
    <>
      {outOfDate && !workout.active && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          action={<Button size="small" onClick={onRebuild} disabled={rebuilding || hasLogs}>Rebuild</Button>}
        >
          Your check-in changed after this session was built.
          {hasLogs && ' Delete the logged sets to rebuild.'}
        </Alert>
      )}

      {rebuilding && <LinearProgress sx={{ mb: 1 }} />}

      {workout.active ? (
        <>
          <Stack spacing={1.5} sx={{ mb: 2 }}>
            {session.exercises.map((item) => (
              <WorkoutCard key={item.position} date={date} item={item}
                           onLogged={() => setRestEndsAt(Date.now() + restSeconds(item) * 1000)} />
            ))}
          </Stack>
          <Button variant="contained" fullWidth sx={{ mb: 2 }} onClick={() => {
            setRestEndsAt(null)
            workout.finish()
            notify('Workout finished')
          }}>
            Finish workout
          </Button>
        </>
      ) : (
        <>
          <Button variant="contained" size="large" fullWidth sx={{ mb: 2 }} onClick={workout.start}>
            {hasLogs ? 'Resume workout' : 'Start workout'}
          </Button>
          <Stack spacing={1} sx={{ mb: 2 }}>
            {session.exercises.map((item) => (
              <ExerciseCard key={item.position} item={item} />
            ))}
          </Stack>
        </>
      )}

      {!workout.active && (
        <>
          <Card variant="outlined" sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="subtitle1" component="h2" sx={{ fontWeight: 600, mb: 1 }}>
                Why today looks like this
              </Typography>
              <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                {session.notes.map((note) => (
                  <Typography component="li" variant="body2" key={note} sx={{ mb: 0.5 }}>{note}</Typography>
                ))}
              </Box>
            </CardContent>
          </Card>
          <Button variant="outlined" fullWidth onClick={onRebuild} disabled={rebuilding || hasLogs}>
            Rebuild session
          </Button>
          {hasLogs && (
            <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 0.5, textAlign: 'center' }}>
              Sets are logged, so this session can no longer be rebuilt.
            </Typography>
          )}
        </>
      )}

      {restEndsAt != null && (
        <RestTimer
          key={restEndsAt}
          endsAt={restEndsAt}
          onAdd={(seconds) => setRestEndsAt(restEndsAt + seconds * 1000)}
          onDone={() => setRestEndsAt(null)}
        />
      )}

    </>
  )
}

/* the overview before the workout starts; logging happens in workout mode */
function ExerciseCard({ item }: { item: SessionExercise }) {
  const done = item.logged.length >= item.sets
  return (
    <Card variant="outlined" sx={{ opacity: item.skipped ? 0.6 : 1 }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
          <Typography color="text.secondary" sx={{ width: 20, flexShrink: 0, fontWeight: 600 }}>{item.position}</Typography>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography sx={{ fontWeight: 600 }}>{item.name}</Typography>
              {item.mainLift && <Chip size="small" variant="outlined" label="Main lift" />}
              {item.skipped && <Chip size="small" label="Skipped" />}
            </Stack>
            <Typography variant="body2">
              {item.sets} × {item.target}
              {item.targetLoadLb != null && ` · ${item.targetLoadLb} lb`}
              {item.targetLoadLb == null && isStrength(item.kind) && ' · Pick a working weight'}
              {` · rest ${clock(restSeconds(item))}`}
            </Typography>
            {item.substitutedFor && (
              <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                <SwapHorizIcon fontSize="small" color="action" />
                <Typography variant="caption" color="text.secondary">Replaces {item.substitutedFor.name}</Typography>
              </Stack>
            )}
            {item.reason && (
              <Typography variant="caption" color="text.secondary" component="p">{item.reason}</Typography>
            )}
            <Stack direction="row" spacing={0.75} useFlexGap sx={{ flexWrap: 'wrap', alignItems: 'center', mt: 1 }}>
              {item.logged.map((set, i) => (
                <Chip
                  key={set.setNumber}
                  size="small"
                  color={set.clean ? 'default' : 'warning'}
                  icon={set.clean ? undefined : <WarningAmberIcon />}
                  label={`${i + 1}: ${describeSet(set, item.kind)}`}
                />
              ))}
              <Typography variant="caption" color={done ? 'success.main' : 'text.secondary'}>
                {item.logged.length}/{item.sets} sets
              </Typography>
            </Stack>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}
