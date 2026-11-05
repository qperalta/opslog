/* Because of Render's free version of the server shutting down, we want to let the user know when the server is trying
 * to wake up. We let them know using MUI's built-in loading prop on Button
 */
import { useSlow } from '../components/useSlow'
import { Typography } from '@mui/material'

export function WakeNotice({ pending }: { pending : boolean }) {
  const slow = useSlow(pending) // we now use global hook

  if(!slow) return null
  return (
    <Typography variant="body2" color="text.secondary" role="status" sx={{ textAlign: 'center' }}>
      The server may be waking up. This can take up to a minute.
    </Typography>
  )
}