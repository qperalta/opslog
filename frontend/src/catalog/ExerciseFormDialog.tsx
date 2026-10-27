import { zodResolver } from '@hookform/resolvers/zod'
import {
  Alert, Autocomplete, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField,
} from '@mui/material'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { labelOf } from '../components/format'
import { useNotify } from '../components/NotifyContext'
import { useEquipment, useExerciseKinds, useMovementPatterns, useMuscleGroups, useSaveExercise } from './queries'
import type { Exercise, ExerciseRequest } from './types'

const isIntBetween = (value: string, min: number, max: number) =>
  /^\d+$/.test(value) && Number(value) >= min && Number(value) <= max

// same limits as ExerciseRequest; the target fields depend on the kind
const schema = z
  .object({
    kind: z.string().min(1, 'Kind is required'),
    name: z.string().trim().min(1, 'Name is required').max(80, 'Name must be 80 characters or fewer'),
    movementPatternId: z.string().min(1, 'Movement pattern is required'),
    primaryMuscleId: z.string().min(1, 'Primary muscle group is required'),
    repMin: z.string().trim(),
    repMax: z.string().trim(),
    targetSeconds: z.string().trim(),
    durationMinutes: z.string().trim(),
    equipmentIds: z.array(z.number()).max(10, 'An exercise can list at most 10 equipment items'),
  })
  .superRefine((values, ctx) => {
    const require = (field: 'repMin' | 'repMax' | 'targetSeconds' | 'durationMinutes', min: number, max: number, message: string) => {
      if (!isIntBetween(values[field], min, max)) ctx.addIssue({ code: 'custom', path: [field], message })
    }
    if (values.kind === 'STRENGTH') {
      require('repMin', 1, 50, 'Minimum reps must be 1 to 50')
      require('repMax', 1, 50, 'Maximum reps must be 1 to 50')
      if (Number(values.repMin) > Number(values.repMax)) {
        ctx.addIssue({ code: 'custom', path: ['repMax'], message: 'Maximum reps must be at least the minimum' })
      }
    }
    if (values.kind === 'TIMED') require('targetSeconds', 1, 600, 'Target time must be 1 to 600 seconds')
    if (values.kind === 'CARDIO') require('durationMinutes', 1, 60, 'Duration must be 1 to 60 minutes')
  })

type ExerciseValues = z.infer<typeof schema>

const FIELDS = ['kind', 'name', 'movementPatternId', 'primaryMuscleId', 'repMin', 'repMax', 'targetSeconds', 'durationMinutes', 'equipmentIds'] as const

function toValues(exercise?: Exercise): ExerciseValues {
  return {
    kind: exercise?.kind ?? '',
    name: exercise?.name ?? '',
    movementPatternId: exercise ? String(exercise.movementPattern.id) : '',
    primaryMuscleId: exercise ? String(exercise.primaryMuscle.id) : '',
    repMin: exercise?.repMin?.toString() ?? '',
    repMax: exercise?.repMax?.toString() ?? '',
    targetSeconds: exercise?.targetSeconds?.toString() ?? '',
    durationMinutes: exercise?.durationMinutes?.toString() ?? '',
    equipmentIds: exercise?.equipment.map((item) => item.id) ?? [],
  }
}

/* only the fields for the chosen kind are sent */
function toRequest(values: ExerciseValues): ExerciseRequest {
  return {
    kind: values.kind,
    name: values.name,
    movementPatternId: Number(values.movementPatternId),
    primaryMuscleId: Number(values.primaryMuscleId),
    repMin: values.kind === 'STRENGTH' ? Number(values.repMin) : undefined,
    repMax: values.kind === 'STRENGTH' ? Number(values.repMax) : undefined,
    targetSeconds: values.kind === 'TIMED' ? Number(values.targetSeconds) : undefined,
    durationMinutes: values.kind === 'CARDIO' ? Number(values.durationMinutes) : undefined,
    equipmentIds: values.equipmentIds,
  }
}

interface Props {
  exercise?: Exercise
  onClose: () => void
}

/* mount with a key so it starts fresh for each exercise */
export function ExerciseFormDialog({ exercise, onClose }: Props) {
  const kinds = useExerciseKinds()
  const patterns = useMovementPatterns()
  const muscles = useMuscleGroups()
  const equipment = useEquipment()
  const save = useSaveExercise()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<ExerciseValues>({
    resolver: zodResolver(schema),
    defaultValues: toValues(exercise),
  })
  const kind = useWatch({ control, name: 'kind' })

  const onSubmit = async (values: ExerciseValues) => {
    setFormError(null)
    try {
      const saved = await save.mutateAsync({ id: exercise?.id, body: toRequest(values) })
      notify(exercise ? `Updated ${saved.name}` : `Added ${saved.name}`)
      onClose()
    } catch (error) {
      setFormError(applyServerErrors(error, setError, FIELDS))
    }
  }

  const select = (name: 'kind' | 'movementPatternId' | 'primaryMuscleId', label: string,
                  options: { value: string; label: string }[], disabled = false) => (
    <Controller
      name={name}
      control={control}
      render={({ field }) => (
        <TextField select label={label} {...field} disabled={disabled}
                   error={!!errors[name]} helperText={errors[name]?.message}>
          {options.map((option) => (
            <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
          ))}
        </TextField>
      )}
    />
  )

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{exercise ? 'Edit exercise' : 'New exercise'}</DialogTitle>
      <DialogContent>
        <Stack component="form" id="exercise-form" spacing={2} sx={{ pt: 1 }} noValidate onSubmit={handleSubmit(onSubmit)}>
          {formError && <Alert severity="error">{formError}</Alert>}
          {select('kind', 'Kind', (kinds.data ?? []).map((value) => ({ value, label: labelOf(value) })), !!exercise)}
          <TextField label="Name" {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
          {select('movementPatternId', 'Movement pattern', (patterns.data ?? []).map((p) => ({ value: String(p.id), label: p.name })))}
          {select('primaryMuscleId', 'Primary muscle', (muscles.data ?? []).map((m) => ({ value: String(m.id), label: m.name })))}
          {kind === 'STRENGTH' && (
            <Stack direction="row" spacing={2}>
              <TextField label="Min reps" type="number" fullWidth {...register('repMin')}
                         error={!!errors.repMin} helperText={errors.repMin?.message} />
              <TextField label="Max reps" type="number" fullWidth {...register('repMax')}
                         error={!!errors.repMax} helperText={errors.repMax?.message} />
            </Stack>
          )}
          {kind === 'TIMED' && (
            <TextField label="Target time (seconds)" type="number" {...register('targetSeconds')}
                       error={!!errors.targetSeconds} helperText={errors.targetSeconds?.message} />
          )}
          {kind === 'CARDIO' && (
            <TextField label="Duration (minutes)" type="number" {...register('durationMinutes')}
                       error={!!errors.durationMinutes} helperText={errors.durationMinutes?.message} />
          )}
          <Controller
            name="equipmentIds"
            control={control}
            render={({ field }) => (
              <Autocomplete
                multiple
                options={equipment.data ?? []}
                groupBy={(item) => labelOf(item.kind)}
                getOptionLabel={(item) => item.name}
                value={(equipment.data ?? []).filter((item) => field.value.includes(item.id))}
                onChange={(_, items) => field.onChange(items.map((item) => item.id))}
                renderInput={(params) => (
                  <TextField {...params} label="Equipment" error={!!errors.equipmentIds}
                             helperText={errors.equipmentIds?.message ?? 'Up to 10 items'} />
                )}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button type="submit" form="exercise-form" variant="contained" disabled={isSubmitting}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}
