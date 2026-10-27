import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert, Button, FormControl, FormHelperText, FormLabel, MenuItem, Stack, TextField, ToggleButton, ToggleButtonGroup,
} from '@mui/material'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useNotify } from '../components/NotifyContext'
import { useLocations } from '../locations/queries'
import { useSaveCheckIn } from './queries'
import type { CheckIn } from './types'

const score = (label: string) =>
  z.number({ error: `${label} is required` })
    .int()
    .min(1, `${label} must be between 1 and 5`)
    .max(5, `${label} must be between 1 and 5`)

const schema = z.object({
  sleepQuality: score('Sleep quality'),
  soreness: score('Soreness'),
  locationId: z.number({ error: 'Location is required' }),
})

type CheckInValues = z.infer<typeof schema>

const SCORES = [1, 2, 3, 4, 5]

interface Props {
  date: string
  existing: CheckIn | null
}

export function CheckInForm({ date, existing }: Props) {
  const locations = useLocations()
  const save = useSaveCheckIn()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<CheckInValues>({
    resolver: zodResolver(schema),
    defaultValues: existing
      ? { sleepQuality: existing.sleepQuality, soreness: existing.soreness, locationId: existing.location?.id }
      : {},
  })

  const onSubmit = async (values: CheckInValues) => {
    setFormError(null)
    try {
      const saved = await save.mutateAsync({ date, body: values })
      notify(`Checked in: ${saved.tier.name} tier today`)
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['sleepQuality', 'soreness', 'locationId']))
    }
  }

  const scoreField = (name: 'sleepQuality' | 'soreness', label: string, low: string, high: string) => (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <FormControl error={!!errors[name]}>
          <FormLabel sx={{ mb: 1 }}>{label}</FormLabel>
          <ToggleButtonGroup
            exclusive
            fullWidth
            color="primary"
            value={field.value ?? null}
            onChange={(_, value: number | null) => {
              if (value !== null) field.onChange(value)
            }}
          >
            {SCORES.map((value) => (
              <ToggleButton key={value} value={value}>
                {value}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
          <FormHelperText>{errors[name]?.message ?? `1 = ${low}, 5 = ${high}`}</FormHelperText>
        </FormControl>
      )}
    />
  )

  return (
    <Stack component="form" spacing={2.5} noValidate onSubmit={handleSubmit(onSubmit)}>
      {formError && <Alert severity="error">{formError}</Alert>}
      {scoreField('sleepQuality', 'How did you sleep?', 'very poorly', 'great')}
      {scoreField('soreness', 'How sore are you?', 'not sore', 'very sore')}
      <Controller
        name="locationId"
        control={control}
        render={({ field }) => (
          <TextField
            select
            label="Where are you training?"
            value={field.value ?? ''}
            onChange={(e) => field.onChange(Number(e.target.value))}
            onBlur={field.onBlur}
            inputRef={field.ref}
            error={!!errors.locationId}
            helperText={errors.locationId?.message}
          >
            {(locations.data ?? []).map((location) => (
              <MenuItem key={location.id} value={location.id}>
                {location.name}
              </MenuItem>
            ))}
          </TextField>
        )}
      />
      <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
        {existing ? 'Update check-in' : 'Check in'}
      </Button>
    </Stack>
  )
}
