import { zodResolver } from '@hookform/resolvers/zod'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  InputAdornment,
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

  const copy = async (text: string, what: string) => {
    try {
      await navigator.clipboard.writeText(text)
      notify(`${what} copied`)
    } catch {
      notify('Copy failed; select the text and copy it by hand', 'error')
    }
  }

  // a read-only value with its own copy button
  const copyField = (label: string, value: string, helperText?: string) => (
    <TextField
      fullWidth
      size="small"
      label={label}
      value={value}
      helperText={helperText}
      slotProps={{
        htmlInput: { readOnly: true },
        input: {
          endAdornment: (
            <InputAdornment position="end">
              <IconButton edge="end" aria-label={`Copy ${label.toLowerCase()}`} onClick={() => copy(value, label)}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </InputAdornment>
          )
        }
      }}
    />
  )

  // second step: the secret is shown once, with what any MCP client needs to connect
  if(secret) {
    const serverUrl = `${window.location.origin}/mcp`
    return (
      <Dialog open onClose={onClose} fullWidth maxWidth="sm">
        <DialogTitle>Connect your AI assistant</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            This is the only time the token is shown. Copy what you need before you close this window.
          </Alert>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {copyField('Server URL', serverUrl)}
            {copyField('Header name', 'Authorization')}
            {copyField('Header value', `Bearer ${secret}`, 'Include the word Bearer and the space after it')}
          </Stack>
          <Typography variant="subtitle2" sx={{ mt: 3, mb: 1 }}>How to connect</Typography>
          <Box component="ol" sx={{ m: 0, pl: 2.5, '& li': { mb: 1 } }}>
            <Typography component="li" variant="body2">
              In your AI assistant, add a custom connector or MCP server. It is usually under Settings, in Connectors or Integrations.
            </Typography>
            <Typography component="li" variant="body2">
              Enter the server URL. If you are asked for a transport, choose Streamable HTTP.
            </Typography>
            <Typography component="li" variant="body2">
              For sign-in, choose none or API key, not OAuth. OpsLog uses this token instead of a sign-in page.
            </Typography>
            <Typography component="li" variant="body2">
              Add a request header with the header name and header value above.
            </Typography>
            <Typography component="li" variant="body2">
              Save, then ask your assistant something like "What is my OpsLog plan this week?" Once it connects, the Plan tab shows a sparkle and the Planner shows what it did.
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 2 }}>
            If your assistant has a Bearer token or API key field instead of request headers, paste only the token there.
          </Typography>
          {copyField('Token only', secret)}
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={onClose}>Done</Button>
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