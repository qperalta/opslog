import { Chip, Stack, Typography } from '@mui/material'
import { useTiers } from './queries'
import { tierColor } from './tierColor'

/* thresholds come from the database, highest tier first */
export function TierRules() {
  const tiers = useTiers()
  return (
    <Stack spacing={1}>
      {[...(tiers.data ?? [])].reverse().map((tier) => (
        <Stack key={tier.id} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Chip size="small" color={tierColor(tier.kind)} label={tier.name} sx={{ minWidth: 80 }} />
          <Typography variant="body2">
            {tier.kind === 'RECOVERY'
              ? 'Anything else'
              : `Sleep ${tier.minSleep} or better and soreness ${tier.maxSoreness} or less`}
            {' · '}
            <Typography component="span" variant="body2" color="text.secondary">{tier.description}</Typography>
          </Typography>
        </Stack>
      ))}
    </Stack>
  )
}
