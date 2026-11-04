/* Because of Render's free version of the server shutting down, we want to let the user know when the server is trying
 * to wake up. We let them know using MUI's built-in loading prop on Button
 */
import { useEffect, useState } from 'react'
import { Typography } from '@mui/material'

export function WakeNotice({ pending }: { pending : boolean }) {
  const [slow, setSlow] = useState(false);

  // pending is the form's isSubmitting (when a user either clicks Sign In or Create Account)
  // This runs whenever it changes:
  useEffect(() => {
    if(!pending) return;
    const timer = setTimeout(() => setSlow(true), 3000)
    return () => {
      clearTimeout(timer)
      setSlow(false)
    }
  }, [pending])

  if(!pending || !slow) return null
  return (
    <Typography variant="body2" color="text.secondary" role="status" sx={{ textAlign: 'center' }}>
      The server may be waking up. This can take up to a minute.
    </Typography>
  )
}