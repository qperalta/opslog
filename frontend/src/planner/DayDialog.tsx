import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, FormControlLabel, FormHelperText,
  FormLabel, MenuItem, Stack, Switch, TextField, ToggleButton, ToggleButtonGroup,
} from '@mui/material'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { formatDay } from '../checkin/dates'
import { useNotify } from '../components/NotifyContext'
import { useLocations } from '../locations/queries'
import { useSaveDay } from './queries'
import type { PlanDay, PlanMode, Workout } from './types'

// 0 = let the rules engine pick / use the program's location
const schema = z.object({
  mode: z.enum(['AUTO', 'TRAINING', 'REST'], { error: 'Mode is required' }),
  workoutId: z.number(),
  locationId: z.number(),
  locked: z.boolean(),
})

type DayValues = z.infer<typeof schema>

const MODES: { value: PlanMode; label: string; help: string }[] = [
  { value: 'AUTO', label: 'Auto', help: 'The rules engine trains on your program days and rests on the others' },
  { value: 'TRAINING', label: 'Train', help: 'Always a training day; pick a workout or take the next one' },
  { value: 'REST', label: 'Rest', help: 'A rest day; later workouts move back one day' },
]

interface Props {
  day: PlanDay
  workouts: Workout[]
  defaultLocation: string | null
  onClose: () => void
}

/* mount with a key so it starts fresh for each day */
export function DayDialog({ day, workouts, defaultLocation, onClose }: Props) {
  const locations = useLocations()
  const save = useSaveDay()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<DayValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      mode: day.mode,
      workoutId: day.mode === 'TRAINING' ? day.workout?.id ?? 0 : 0,
      locationId: day.location?.id ?? 0,
      locked: day.locked,
    },
  })
  const mode = useWatch({ control, name: 'mode' })

  const onSubmit = async (values: DayValues) => {
    setFormError(null)
    try {
      await save.mutateAsync({
        date: day.date,
        body: {
          mode: values.mode,
          workoutId: values.mode === 'TRAINING' && values.workoutId ? values.workoutId : null,
          locationId: values.locationId || null,
          locked: values.locked,
        },
      })
      notify(`Saved ${formatDay(day.date)}`)
      onClose()
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['mode', 'workoutId', 'locationId', 'locked']))
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{formatDay(day.date, { weekday: 'long', month: 'long', day: 'numeric' })}</DialogTitle>
      <DialogContent>
        <Stack component="form" id="day-form" spacing={2.5} sx={{ pt: 1 }} noValidate onSubmit={handleSubmit(onSubmit)}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <Controller
            name="mode"
            control={control}
            render={({ field }) => (
              <FormControl error={!!errors.mode}>
                <FormLabel sx={{ mb: 1 }}>Day type</FormLabel>
                <ToggleButtonGroup
                  exclusive
                  fullWidth
                  color="primary"
                  value={field.value}
                  onChange={(_, value: PlanMode | null) => {
                    if (value !== null) field.onChange(value)
                  }}
                >
                  {MODES.map((option) => (
                    <ToggleButton key={option.value} value={option.value}>
                      {option.label}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
                <FormHelperText>{errors.mode?.message ?? MODES.find((option) => option.value === field.value)?.help}</FormHelperText>
              </FormControl>
            )}
          />
          {mode === 'TRAINING' && (
            <Controller
              name="workoutId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  label="Workout"
                  value={field.value}
                  onChange={(e) => field.onChange(Number(e.target.value))}
                  onBlur={field.onBlur}
                  inputRef={field.ref}
                  error={!!errors.workoutId}
                  helperText={errors.workoutId?.message}
                >
                  <MenuItem value={0}>Next in rotation</MenuItem>
                  {workouts.map((workout) => (
                    <MenuItem key={workout.id} value={workout.id}>
                      {workout.name}
                    </MenuItem>
                  ))}
                </TextField>
              )}
            />
          )}
          <Controller
            name="locationId"
            control={control}
            render={({ field }) => (
              <TextField
                select
                label="Location"
                value={field.value}
                onChange={(e) => field.onChange(Number(e.target.value))}
                onBlur={field.onBlur}
                inputRef={field.ref}
                error={!!errors.locationId}
                helperText={errors.locationId?.message}
              >
                <MenuItem value={0}>{defaultLocation ? `Usual (${defaultLocation})` : 'Usual location'}</MenuItem>
                {(locations.data ?? []).map((location) => (
                  <MenuItem key={location.id} value={location.id}>
                    {location.name}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <Controller
            name="locked"
            control={control}
            render={({ field }) => (
              <FormControl>
                <FormControlLabel
                  control={<Switch checked={field.value} onChange={(e) => field.onChange(e.target.checked)} />}
                  label="Lock this day"
                />
                <FormHelperText sx={{ mt: 0 }}>The rules engine and your AI coach cannot change a locked day</FormHelperText>
              </FormControl>
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button type="submit" form="day-form" variant="contained" disabled={isSubmitting}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}
