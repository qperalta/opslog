import AddIcon from '@mui/icons-material/Add'
import BlockIcon from '@mui/icons-material/Block'
import ReplayIcon from '@mui/icons-material/Replay'
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import { Box, Button, Card, CardContent, Chip, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import { useNotify } from '../components/NotifyContext'
import { HoldTimer } from './HoldTimer'
import { useSetSkipped } from './queries'
import { clock, holdSeconds, isStrength, measureLabel, restSeconds } from './setFormat'
import { SetRow } from './SetRow'
import { TimingEditor } from './TimingEditor'
import type { SessionExercise } from './types'

interface Props {
  date: string
  item: SessionExercise
  onLogged: () => void
}

// same cap as LoggedSet.MAX_SETS
const MAX_SETS = 10

/* one row per planned set plus any added with + Set; only the first set not yet logged takes input */
export function WorkoutCard({ date, item, onLogged }: Props) {
  const skip = useSetSkipped()
  const notify = useNotify()
  const [added, setAdded] = useState(0)
  // seconds from the hold timer, and a count that remounts the active row so it takes them
  const [held, setHeld] = useState({ seconds: '', count: 0 })
  const rows = Math.min(Math.max(item.sets + added, item.logged.length), MAX_SETS)
  const open = rows - item.logged.length
  const hold = holdSeconds(item)
  const [editingTimes, setEditingTimes] = useState(false)

  const setSkipped = (skipped: boolean) =>
    skip.mutate({ date, position: item.position, skipped }, {
      onSuccess: () => notify(skipped ? `Skipped ${item.name}` : `${item.name} is back on`),
      onError: (failure) => notify(failure.message, 'error'),
    })

  if (item.skipped) {
    return (
      <Card variant="outlined" sx={{ bgcolor: 'action.hover' }}>
        <CardContent>
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
            <Typography sx={{ fontWeight: 600, color: 'text.secondary' }}>{item.name}</Typography>
            <Chip size="small" icon={<BlockIcon />} label="Skipped" />
          </Stack>
          <Typography variant="body2" color="text.secondary">{item.sets} × {item.target}</Typography>
          <Button size="small" variant="outlined" startIcon={<ReplayIcon />} sx={{ mt: 1.5 }}
                  onClick={() => setSkipped(false)} disabled={skip.isPending}>
            Re-activate
          </Button>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography sx={{ fontWeight: 600 }}>{item.name}</Typography>
          {item.mainLift && <Chip size="small" variant="outlined" label="Main lift" />}
          {item.adHoc && <Chip size="small" color="primary" variant="outlined" label="Added" />}
        </Stack>
        <Typography variant="body2">
          {item.sets} × {item.target}
          {item.targetLoadLb != null && ` · ${item.targetLoadLb} lb`}
          {item.targetLoadLb == null && isStrength(item.kind) && ' · Pick a working weight'}
          {` · rest ${clock(restSeconds(item))}`}
        </Typography>
        {/* the member's own timer and rest, saved for this exercise */}
        {editingTimes ? (
          <TimingEditor item={item} onDone={() => setEditingTimes(false)} />
        ) : (
          <Button size="small" color="inherit" startIcon={<TimerOutlinedIcon />} onClick={() => setEditingTimes(true)}
                  sx={{ mt: 0.5, px: 0.5, color: 'text.secondary' }}>
            {hold > 0 ? `Timer ${clock(hold)} · ` : ''}Rest {clock(restSeconds(item))}
          </Button>
        )}
        {item.substitutedFor && (
          <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
            <SwapHorizIcon fontSize="small" color="action" />
            <Typography variant="caption" color="text.secondary">Replaces {item.substitutedFor.name}</Typography>
          </Stack>
        )}

        <Stack direction="row" spacing={1} sx={{ mt: 1.5, mb: 0.5 }}>
          <Box sx={{ width: 20 }} />
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>Weight (lb)</Typography>
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>{measureLabel(item.kind)}</Typography>
          <Box sx={{ width: 96 }} />
        </Stack>
        <Stack spacing={1}>
          {item.logged.map((set, i) => (
            <SetRow key={`set-${set.setNumber}`} date={date} item={item} number={i + 1} set={set} active={false}
                    onLogged={onLogged} />
          ))}
          {Array.from({ length: open }, (_, i) => {
            const number = item.logged.length + i + 1
            return (
              <SetRow key={i === 0 ? `open-0-${held.count}` : `open-${i}`} date={date} item={item} number={number}
                      active={i === 0} initialMeasure={i === 0 ? held.seconds : ''}
                      onRemove={number > item.sets ? () => setAdded(Math.max(added - 1, 0)) : undefined}
                      onLogged={() => {
                        setHeld({ seconds: '', count: held.count + 1 })
                        onLogged()
                      }} />
            )
          })}
        </Stack>

        <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
          {hold > 0 && open > 0 && (
            <HoldTimer date={date} storageKey={`opslog.hold.${item.id}`} seconds={hold}
                       onDone={(seconds) => setHeld({ seconds: String(seconds), count: held.count + 1 })} />
          )}
          <Button size="small" variant="outlined" startIcon={<AddIcon />} disabled={rows >= MAX_SETS}
                  onClick={() => setAdded(added + 1)}>
            Set
          </Button>
          {item.logged.length === 0 && (
            <Button size="small" color="inherit" startIcon={<BlockIcon />} onClick={() => setSkipped(true)}
                    disabled={skip.isPending}>
              Skip
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}
