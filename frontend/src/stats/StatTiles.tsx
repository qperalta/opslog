import TrendingDownIcon from '@mui/icons-material/TrendingDown'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import { Box, Paper, Stack, Typography } from '@mui/material'
import { compact } from './periods'
import type { Totals } from './periods'

const TILES: { key: keyof Totals; label: string }[] = [
  { key: 'workouts', label: 'Workouts' },
  { key: 'sets', label: 'Sets' },
  { key: 'lifted', label: 'Lifted (lb)' },
]

function Delta({ change }: { change: number }) {
  if (change === 0) return <Typography variant="caption" color="text.secondary">No change</Typography>
  const up = change > 0
  const Icon = up ? TrendingUpIcon : TrendingDownIcon
  return (
    <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', color: up ? 'success.main' : 'text.secondary' }}>
      <Icon sx={{ fontSize: 16 }} />
      <Typography variant="caption" color="text.secondary">{up ? '+' : '−'}{compact(Math.abs(change))}</Typography>
    </Stack>
  )
}

/* the same tile row as the page headers; the change line compares to the same point in the period before */
export function StatTiles({ current, previous, versus }: { current: Totals; previous: Totals | null; versus: string | null }) {
  return (
    <Box sx={{ mb: 2 }}>
      <Paper variant="outlined" sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
        gap: '1px',
        bgcolor: 'divider',
        overflow: 'hidden',
      }}>
        {TILES.map(({ key, label }) => (
          <Box key={key} sx={{ bgcolor: 'background.paper', px: 1.5, py: 1.5, minWidth: 0 }}>
            <Typography variant="caption" color="text.secondary" sx={{
              display: 'block',
              fontWeight: 600,
              textTransform: 'uppercase',
              letterSpacing: '0.06em',
            }}>
              {label}
            </Typography>
            <Typography component="p" noWrap sx={{ fontWeight: 700, fontSize: '1.3rem', lineHeight: 1.3 }}>
              {compact(current[key])}
            </Typography>
            {previous && <Delta change={current[key] - previous[key]} />}
          </Box>
        ))}
      </Paper>
      {previous && versus && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5, px: 0.5 }}>
          Change vs. the same point {versus}
        </Typography>
      )}
    </Box>
  )
}
