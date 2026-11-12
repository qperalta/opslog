import { zodResolver } from '@hookform/resolvers/zod'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Stack,
  Switch,
  TextField,
  Typography
} from '@mui/material'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useNotify } from '../components/NotifyContext'
import { useIssueMcpToken } from './queries'

// same limits as McpTokenRequest
const schema = z.object({
  name: z.string().trim()
    .min(1, 'Name is required')
    .max(60, 'Name must be 60 characters or fewer'),
  canWrite: z.boolean()
})

type TokenValues = z.infer<typeof schema>

// two steps: name the token then show the secret once, closing throws it away after
export function TokenFormDialog({ onClose }: { onClose: () => void }) {
  const issue = useIssueMcpToken()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const [secret, setSecret] = useState<string | null>(null)
  const { control, register, handleSubmit, setError, formState: { errors, isSubmitting} } = useForm<TokenValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', canWrite: false }
  })

  const onSubmit = async (values: TokenValues) => {
    setFormError(null)
    try {
      const issued = await issue.mutateAsync({
        name: values.name,
        scope: values.canWrite
          ? 'READ_WRITE'
          : 'READ'
      })
      setSecret(issued.secret)
      notify(`Created ${issued.token.name}`)
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['name']))
    }
  }

  const copy = async () => {
    if(!secret) return
    try {
      await navigator.clipboard.writeText(secret)
      notify('Token copied')
    } catch {
      notify('Copy failed; select the token and copy it by hand', 'error')
    }
  }

  if(secret) {
    return (
      <Dialog open onClose={onClose} fullWidth maxWidth="sm">
        <DialogTitle>Copy your token</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            This is the only time the token is shown. Paste it into your AI assistant's settings now.
          </Alert>
          <TextField
            fullWidth
            label="Token"
            value={secret}
            slotProps={{ htmlInput: { readOnly: true } }}
          />
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mt: 2 }}>
            Server URL: {window.location.origin}/mcp
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button
            startIcon={<ContentCopyIcon/>}
            onClick={copy}
          >Copy</Button>
          <Button
            variant="contained"
            onClick={onClose}
          >Done</Button>
        </DialogActions>
      </Dialog>
    )
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>New AI access token</DialogTitle>
      <DialogContent>
        <Stack
          component="form"
          id="token-form"
          spacing={2}
          sx={{ pt: 1}}
          noValidate
          onSubmit={handleSubmit(onSubmit)}
        >
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField
            label="Name"
            placeholder="Claude desktop" {...register('name')}
            error={!!errors.name}
            helperText={errors.name?.message ?? 'So you can tell your tokens apart'} />
          <Controller
            name="canWrite"
            control={control}
            render={({ field}) => (
              <FormControlLabel
                control={<Switch checked={field.value} onChange={(event) =>
                  field.onChange(event.target.checked)} />}
                label="Allow changes to my plan and sessions" />
            )}
          />
          <Typography variant="body2" color="text.secondary">
            Off: the assistant can only read your check-ins, logs, plan and reports. Locked days stay read-only either way.
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button type="submit" form="token-form" variant="contained" disabled={isSubmitting}>Create token</Button>
      </DialogActions>
    </Dialog>
  )
}