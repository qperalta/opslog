import { Alert, Dialog, DialogContent, DialogTitle } from '@mui/material'
import { CheckInForm } from './CheckInForm'
import { formatDay, todayLocal } from './dates'
import type { CheckIn } from './types'

interface Props {
  checkIn: CheckIn
  onClose: () => void
}

export function CheckInDialog({ checkIn, onClose }: Props) {
  const past = checkIn.date < todayLocal()
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Check-in · {formatDay(checkIn.date)}</DialogTitle>
      <DialogContent>
        {/* sessions are stored as built, so a past day keeps its tier */}
        {past && <Alert severity="info" sx={{ mb: 2 }}>The session for this day keeps the tier it was built with.</Alert>}
        <CheckInForm date={checkIn.date} existing={checkIn} onSaved={onClose} />
      </DialogContent>
    </Dialog>
  )
}
