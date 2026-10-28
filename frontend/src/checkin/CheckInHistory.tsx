import {
  Alert, Card, Chip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from '@mui/material'
import { useState } from 'react'
import { CheckInDialog } from './CheckInDialog'
import { formatDay } from './dates'
import { useCheckIns } from './queries'
import { tierColor } from './tierColor'
import type { CheckIn } from './types'

/* tap a row to edit that day's check-in */
export function CheckInHistory() {
  const history = useCheckIns()
  const [editing, setEditing] = useState<CheckIn | null>(null)

  return (
    <>
      <Typography variant="h6" component="h2" sx={{ mb: 0.5 }}>
        Last 14 days
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        Tap a day to edit its check-in.
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
                <TableRow key={checkIn.id} hover onClick={() => setEditing(checkIn)} sx={{ cursor: 'pointer' }}
                          aria-label={`Edit check-in for ${formatDay(checkIn.date)}`}>
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
      {editing && <CheckInDialog key={editing.id} checkIn={editing} onClose={() => setEditing(null)} />}
    </>
  )
}
