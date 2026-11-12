import AddIcon from '@mui/icons-material/Add'
import { Alert, Box, Button, Card, CardContent, Chip, LinearProgress, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useNotify } from '../components/NotifyContext'
import { useMcpTokens, useRevokeMcpToken } from './queries'
import { TokenFormDialog } from './TokenFormDialog'
import type { McpToken } from './types'

const when = (instant: string) =>
  new Date(instant).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })

const usage = (token: McpToken) => {
  if(token.revokedAt) return `Revoked ${when(token.revokedAt)}`
  return token.lastUsedAt
    ? `Last used ${when(token.lastUsedAt)}`
    : 'Never used'
}

export function AiAccessSection() {
  const tokens = useMcpTokens()
  const revoke = useRevokeMcpToken()
  const notify = useNotify()
  const [creating, setCreating] = useState(false)
  const [revoking, setRevoking] = useState<McpToken | null>(null)

  const confirmRevoke = async () => {
    if(!revoking) return
    try {
      await revoke.mutateAsync(revoking.id)
      notify(`Revoked ${revoking.name}`)
    } catch (error) {
      notify(error instanceof ApiError
        ? error.message
        : 'Could not revoke the token', 'error')
    }
    setRevoking(null)
  }

  return (
    <Box sx={{ mt: 3 }}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h6" component="h2">AI access</Typography>
        <Button startIcon={<AddIcon />} variant="contained" size="small" onClick={() => setCreating(true)}>New</Button>
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Tokens let an AI assistant read your training and, if allowed, plan days and write sessions. OpsLog works fully without one.
      </Typography>

      {tokens.isPending && <LinearProgress />}
      {tokens.isError && <Alert severity="error">{tokens.error.message}</Alert>}
      {tokens.data?.length === 0 && <Alert severity="info">No tokens yet.</Alert>}

      <Stack spacing={1}>
        {tokens.data?.map((token) => (
          <Card key={token.id} variant="outlined" sx={{ opacity: token.active ? 1 : 0.6 }}>
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                    <Typography sx={{ fontWeight: 600 }} noWrap>{token.name}</Typography>
                    <Chip size="small" variant="outlined"
                          label={token.scope === 'READ_WRITE' ? 'Read & write' : 'Read only'} />
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    Created {when(token.createdAt)} · {usage(token)}
                  </Typography>
                </Box>
                {token.active && (
                  <Button size="small" color="error" onClick={() => setRevoking(token)} sx={{ flexShrink: 0 }}>
                    Revoke
                  </Button>
                )}
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {creating && <TokenFormDialog onClose={() => setCreating(false)} />}
      <ConfirmDialog
        open={revoking !== null}
        title="Revoke token?"
        message={`${revoking?.name ?? ''} stops working right away. Any assistant using it loses access.`}
        confirmLabel="Revoke"
        busy={revoke.isPending}
        onConfirm={confirmRevoke}
        onClose={() => setRevoking(null)}
      />
    </Box>
  )
}