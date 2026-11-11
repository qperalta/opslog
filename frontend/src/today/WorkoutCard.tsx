import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import { Box, Card, CardContent, Chip, Stack, Typography } from '@mui/material'
import { clock, isStrength, measureLabel, restSeconds } from './setFormat'
import { SetRow } from './SetRow'
import type { SessionExercise } from './types'

interface Props {
  date: string
  item: SessionExercise
  onLogged: () => void
}

/* one row per planned set; only the first set not yet logged takes input */
export function WorkoutCard({ date, item, onLogged }: Props) {
  const open = Math.max(item.sets - item.logged.length, 0)

  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
          <Typography sx={{ fontWeight: 600 }}>{item.name}</Typography>
          {item.mainLift && <Chip size="small" variant="outlined" label="Main lift" />}
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
          {Array.from({ length: open }, (_, i) => (
            <SetRow key={`open-${i}`} date={date} item={item} number={item.logged.length + i + 1} active={i === 0}
                    onLogged={onLogged} />
          ))}
        </Stack>
      </CardContent>
    </Card>
  )
}
