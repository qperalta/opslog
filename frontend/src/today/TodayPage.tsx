import AddIcon from '@mui/icons-material/Add'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import {
  Alert, AlertTitle, Box, Button, Card, CardContent, Chip, LinearProgress, Stack, Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink } from 'react-router'
import { ApiError } from '../api/client'
import { formatDay, todayLocal } from '../checkin/dates'
import { useCheckIn } from '../checkin/queries'
import { tierColor } from '../checkin/tierColor'
import { useNotify } from '../components/NotifyContext'
import { PageHeader } from '../components/PageHeader'
import { useRebuildSession, useSession } from './queries'
import { RestTimer } from './RestTimer'
import { SetDialog } from './SetDialog'
import { describeSet, isStrength } from './setFormat'
import type { LoggedSet, Session, SessionExercise } from './types'

const BUILT_BY = { USER: 'Built by you', RULES: 'Built by the rules engine', AGENT: 'Written by your AI coach' }

// main lifts get the longest rest
const restSeconds = (item: SessionExercise) => item.mainLift ? 180 : isStrength(item.kind) ? 90 : 60

interface Editing {
  item: SessionExercise
  set?: LoggedSet
}

export function TodayPage() {
  const today = todayLocal()
  const session = useSession(today)
  const checkIn = useCheckIn(today)
  const rebuild = useRebuildSession()
  const notify = useNotify()

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

  return (
    <>
      <PageHeader title="Today" />
      <Typography color="text.secondary" sx={{ mt: -1.5, mb: 2 }}>
        {formatDay(today, { weekday: 'long', month: 'long', day: 'numeric' })}
      </Typography>

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
        <Alert severity="warning" action={<Button component={RouterLink} to="/checkin" size="small" sx={{ whiteSpace: 'nowrap' }}>Check in</Button>}>
          <AlertTitle>{session.data.workout?.name ?? 'Training'} today</AlertTitle>
          Check in first. Your sleep and soreness set the tier, and your location sets the equipment.
        </Alert>
      )}

      {session.data?.status === 'READY' && (
        <ReadySession date={today} session={session.data} outOfDate={outOfDate} rebuilding={rebuild.isPending} onRebuild={onRebuild} />
      )}
    </>
  )
}

interface ReadyProps {
  date: string
  session: Session
  outOfDate: boolean
  rebuilding: boolean
  onRebuild: () => void
}

function ReadySession({ date, session, outOfDate, rebuilding, onRebuild }: ReadyProps) {
  const [editing, setEditing] = useState<Editing | null>(null)
  const [restEndsAt, setRestEndsAt] = useState<number | null>(null)

  // rebuilding would delete the log, so the server refuses it
  const hasLogs = session.exercises.some((item) => item.logged.length > 0)

  return (
    <>
      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 0.5 }}>
            <Typography variant="h6" component="h2">{session.workout?.name ?? 'Session'}</Typography>
            {session.tier && <Chip size="small" color={tierColor(session.tierKind ?? '')} label={session.tier.name} />}
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {session.location?.name ?? 'No location'} · {session.exercises.length} exercises
            {session.source && ` · ${BUILT_BY[session.source]}`}
          </Typography>
        </CardContent>
      </Card>

      {outOfDate && (
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

      <Stack spacing={1} sx={{ mb: 2 }}>
        {session.exercises.map((item) => (
          <ExerciseCard key={item.position} item={item}
                        onLog={() => setEditing({ item })}
                        onEdit={(set) => setEditing({ item, set })} />
        ))}
      </Stack>

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

      {restEndsAt != null && (
        <RestTimer
          key={restEndsAt}
          endsAt={restEndsAt}
          onAdd={(seconds) => setRestEndsAt(restEndsAt + seconds * 1000)}
          onDone={() => setRestEndsAt(null)}
        />
      )}

      {editing && (
        <SetDialog
          key={`${editing.item.position}-${editing.set?.setNumber ?? 'new'}`}
          date={date}
          item={editing.item}
          set={editing.set}
          onLogged={() => setRestEndsAt(Date.now() + restSeconds(editing.item) * 1000)}
          onClose={() => setEditing(null)}
        />
      )}
    </>
  )
}

interface ExerciseProps {
  item: SessionExercise
  onLog: () => void
  onEdit: (set: LoggedSet) => void
}

function ExerciseCard({ item, onLog, onEdit }: ExerciseProps) {
  const done = item.logged.length >= item.sets
  return (
    <Card variant="outlined">
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
          <Typography color="text.secondary" sx={{ width: 20, flexShrink: 0, fontWeight: 600 }}>{item.position}</Typography>
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography sx={{ fontWeight: 600 }}>{item.name}</Typography>
              {item.mainLift && <Chip size="small" variant="outlined" label="Main lift" />}
            </Stack>
            <Typography variant="body2">
              {item.sets} × {item.target}
              {item.targetLoadLb != null && ` · ${item.targetLoadLb} lb`}
              {item.targetLoadLb == null && isStrength(item.kind) && ' · Pick a working weight'}
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
              {item.logged.map((set) => (
                <Chip
                  key={set.setNumber}
                  size="small"
                  color={set.clean ? 'default' : 'warning'}
                  icon={set.clean ? undefined : <WarningAmberIcon />}
                  label={`${set.setNumber}: ${describeSet(set, item.kind)}`}
                  onClick={() => onEdit(set)}
                  aria-label={`Edit set ${set.setNumber}`}
                />
              ))}
              <Button size="small" startIcon={<AddIcon />} onClick={onLog} disabled={item.logged.length >= 10}
                      variant={done ? 'text' : 'outlined'}>
                Log set
              </Button>
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
