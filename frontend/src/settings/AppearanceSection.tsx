import { Card, CardContent, Stack, Switch, Typography } from '@mui/material'
import { useColorScheme } from '@mui/material/styles'

export const AppearanceSection = () => {
  const { mode, systemMode, setMode } = useColorScheme()
  const current = mode === 'system' ? systemMode : mode

  return (
    <>
      <Typography variant="h6" component="h2" sx={{ mb: 1 }}>Appearance</Typography>
      <Card sx={{ mb: 3}}>
        <CardContent>
          <Stack
            direction="row"
            spacing={2}
            sx={{
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <Typography component="label" htmlFor="dark-mode" sx={{ fontWeight: 600 }}>
              Dark Mode
            </Typography>
            <Switch
              id="dark-mode"
              checked={current === 'dark'}
              disabled={!current}
              onChange={(e) => setMode(e.target.checked ? 'dark' : 'light' )}
            />
          </Stack>
        </CardContent>
      </Card>
    </>
  )
}