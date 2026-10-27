import {
  Alert, AlertTitle, Card, CardContent, Chip, LinearProgress, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Typography,
} from '@mui/material'
import { PageHeader } from '../components/PageHeader'
import { CheckInForm } from './CheckInForm'
import { formatDay, todayLocal } from './dates'
import { useCheckIn, useCheckIns, useTiers } from './queries'
import { tierColor } from './tierColor'

export function CheckInPage() {
  const today = todayLocal()
  const current = useCheckIn(today)
  const history = useCheckIns()
  const tiers = useTiers()

  return (
    <>
      <PageHeader title="Morning check-in" />
      <Typography color="text.secondary" sx={{ mt: -1.5, mb: 2 }}>
        {formatDay(today, { weekday: 'long', month: 'long', day: 'numeric' })}
      </Typography>

      {current.data && (
        <Alert severity={tierColor(current.data.tier.kind)} sx={{ mb: 2 }}>
          <AlertTitle>Today: {current.data.tier.name} tier</AlertTitle>
          {current.data.tier.description}
        </Alert>
      )}

      <Card sx={{ mb: 2 }}>
        <CardContent>
          {current.isPending && <LinearProgress />}
          {current.isError && <Alert severity="error">{current.error.message}</Alert>}
          {/* key by date so the form keeps its state while the check-in refetches after a save */}
          {current.isSuccess && <CheckInForm key={today} date={today} existing={current.data} />}
        </CardContent>
      </Card>

      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>
        How the tier is picked
      </Typography>
      <Stack spacing={1} sx={{ mb: 3 }}>
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

      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>
        Last 14 days
      </Typography>
      {history.data?.length === 0 && <Alert severity="info">No check-ins yet.</Alert>}
      {!!history.data?.length && (
        <TableContainer component={Card}>
          <Table size="small" sx={{ '& td, & th': { px: 1 } }}>
            <TableHead>
              <TableRow>
                <TableCell>Date</TableCell>
                <TableCell align="center">Sleep</TableCell>
                <TableCell align="center">Sore</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>Tier</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.data.map((checkIn) => (
                <TableRow key={checkIn.id}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDay(checkIn.date, { month: 'short', day: 'numeric' })}</TableCell>
                  <TableCell align="center">{checkIn.sleepQuality}</TableCell>
                  <TableCell align="center">{checkIn.soreness}</TableCell>
                  <TableCell>{checkIn.location?.name ?? '(deleted)'}</TableCell>
                  <TableCell>
                    <Chip size="small" color={tierColor(checkIn.tier.kind)} label={checkIn.tier.name} />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  )
}
