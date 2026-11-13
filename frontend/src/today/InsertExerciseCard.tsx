import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Autocomplete, Button, Card, CardContent, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useDeferredValue, useEffect, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useExerciseKinds, useExercises, useMovementPatterns, useMuscleGroups } from '../catalog/queries'
import type { Exercise, ExerciseRequest } from '../catalog/types'
import { labelOf } from '../components/format'
import { useNotify } from '../components/NotifyContext'
import { useInsertExercise } from './queries'
import { parseClock } from './setFormat'

const inRange = (seconds: number | null, min: number, max: number) => seconds != null && seconds >= min && seconds <= max

const isIntBetween = (value: string, min: number, max: number) =>
  /^\d+$/.test(value) && Number(value) >= min && Number(value) <= max

// same limits as InsertExerciseRequest and ExerciseRequest; a new name also needs what a custom exercise needs
const schema = z
  .object({
    name: z.string().trim().min(1, 'Pick or type an exercise').max(80, 'Name must be 80 characters or fewer'),
    isNew: z.boolean(),
    sets: z.string().trim().refine((value) => isIntBetween(value, 1, 10), 'Sets must be 1 to 10'),
    targetLoadLb: z.string().trim()
      .refine((value) => value === '' || (Number.isFinite(Number(value)) && Number(value) >= 0 && Number(value) <= 2000),
        'Weight must be 0 to 2000 lb'),
    kind: z.string(),
    movementPatternId: z.string(),
    primaryMuscleId: z.string(),
    target: z.string().trim(),
    // M:SS or seconds; blank keeps the member's own or the default
    hold: z.string().trim().refine((value) => value === '' || inRange(parseClock(value), 1, 600), 'Timer must be 0:01 to 10:00'),
    rest: z.string().trim().refine((value) => value === '' || inRange(parseClock(value), 0, 900), 'Rest must be 0:00 to 15:00'),
  })
  .superRefine((values, ctx) => {
    if (!values.isNew) return
    const issue = (path: string, message: string) => ctx.addIssue({ code: 'custom', path: [path], message })
    if (!values.kind) issue('kind', 'Kind is required')
    if (!values.movementPatternId) issue('movementPatternId', 'Movement pattern is required')
    if (!values.primaryMuscleId) issue('primaryMuscleId', 'Primary muscle group is required')
    if (values.kind === 'STRENGTH' && !isIntBetween(values.target, 1, 50)) issue('target', 'Reps must be 1 to 50')
    if (values.kind === 'TIMED' && !isIntBetween(values.target, 1, 600)) issue('target', 'Seconds must be 1 to 600')
    if (values.kind === 'CARDIO' && !isIntBetween(values.target, 1, 60)) issue('target', 'Minutes must be 1 to 60')
  })

type InsertValues = z.infer<typeof schema>

const FIELDS = ['sets', 'targetLoadLb', 'hold', 'rest'] as const

const TARGET_LABEL: Record<string, string> = { STRENGTH: 'Reps', TIMED: 'Seconds', CARDIO: 'Minutes' }

// a new strength exercise gets the reps as its whole rep range
function newExercise(values: InsertValues): ExerciseRequest {
  const target = Number(values.target)
  return {
    kind: values.kind,
    name: values.name,
    movementPatternId: Number(values.movementPatternId),
    primaryMuscleId: Number(values.primaryMuscleId),
    repMin: values.kind === 'STRENGTH' ? target : undefined,
    repMax: values.kind === 'STRENGTH' ? target : undefined,
    targetSeconds: values.kind === 'TIMED' ? target : undefined,
    durationMinutes: values.kind === 'CARDIO' ? target : undefined,
    equipmentIds: [],
  }
}

interface Props {
  date: string
  position: number
  onDone: () => void
}

/* in place between two workout cards; saving inserts the exercise at this position */
export function InsertExerciseCard({ date, position, onDone }: Props) {
  const kinds = useExerciseKinds()
  const patterns = useMovementPatterns()
  const muscles = useMuscleGroups()
  const insert = useInsertExercise()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, register, handleSubmit, setValue, setError, formState: { errors, isSubmitting } } = useForm<InsertValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '', isNew: false, sets: '3', targetLoadLb: '', kind: 'STRENGTH', movementPatternId: '', primaryMuscleId: '', target: '10',
      hold: '', rest: '',
    },
  })
  const name = useWatch({ control, name: 'name' })
  const kind = useWatch({ control, name: 'kind' })

  // the list is searched on the server as you type; a name with no exact match (any case) is a new exercise
  const query = useDeferredValue(name.trim())
  const options = useExercises({ q: query || undefined, page: 0, size: 20 })
  const found = (text: string) =>
    options.data?.items.find((option) => option.name.toLowerCase() === text.trim().toLowerCase())
  const isNew = name.trim() !== '' && query === name.trim() && !options.isFetching && !found(name)

  // only timed exercises get a hold timer
  const timed = (found(name)?.kind ?? (isNew ? kind : '')) === 'TIMED'

  // the schema only asks for kind, pattern and muscle when the name is new
  useEffect(() => setValue('isNew', isNew), [isNew, setValue])

  const onSubmit = async (values: InsertValues) => {
    setFormError(null)
    const match = found(values.name)
    try {
      await insert.mutateAsync({
        date,
        body: {
          position,
          exerciseId: match ? match.id : null,
          newExercise: match ? null : newExercise(values),
          sets: Number(values.sets),
          targetLoadLb: values.targetLoadLb === '' ? null : Number(values.targetLoadLb),
          holdSeconds: timed && values.hold !== '' ? parseClock(values.hold) : null,
          restSeconds: values.rest !== '' ? parseClock(values.rest) : null,
        },
      })
      notify(`Added ${match?.name ?? values.name.trim()}`)
      onDone()
    } catch (error) {
      setFormError(applyServerErrors(error, setError, FIELDS))
    }
  }

  const select = (field: 'kind' | 'movementPatternId' | 'primaryMuscleId', label: string,
                  choices: { value: string; label: string }[]) => (
    <Controller
      name={field}
      control={control}
      render={({ field: input }) => (
        <TextField select size="small" fullWidth label={label} {...input}
                   error={!!errors[field]} helperText={errors[field]?.message}>
          {choices.map((choice) => <MenuItem key={choice.value} value={choice.value}>{choice.label}</MenuItem>)}
        </TextField>
      )}
    />
  )

  return (
    <Card variant="outlined" sx={{ borderColor: 'primary.main' }}>
      <CardContent>
        <Stack component="form" spacing={1.5} noValidate onSubmit={handleSubmit(onSubmit)}>
          <Typography sx={{ fontWeight: 600 }}>Add an exercise here</Typography>
          {formError && <Alert severity="error">{formError}</Alert>}
          <Autocomplete<Exercise, false, false, true>
            freeSolo
            options={options.data?.items ?? []}
            getOptionLabel={(option) => typeof option === 'string' ? option : option.name}
            filterOptions={(list) => list}
            inputValue={name}
            onInputChange={(_, text) => setValue('name', text)}
            loading={options.isFetching}
            renderInput={(params) => (
              <TextField {...params} size="small" label="Exercise" autoFocus
                         error={!!errors.name}
                         helperText={errors.name?.message ?? (isNew ? 'Not in the list yet: it will be added as your own exercise' : ' ')} />
            )}
          />
          {isNew && (
            <>
              {select('kind', 'Kind', (kinds.data ?? []).map((value) => ({ value, label: labelOf(value) })))}
              {select('movementPatternId', 'Movement pattern', (patterns.data ?? []).map((p) => ({ value: String(p.id), label: p.name })))}
              {select('primaryMuscleId', 'Primary muscle', (muscles.data ?? []).map((m) => ({ value: String(m.id), label: m.name })))}
            </>
          )}
          <Stack direction="row" spacing={1}>
            <TextField size="small" label="Sets" slotProps={{ inputLabel: { shrink: true } }} type="number" fullWidth {...register('sets')}
                       error={!!errors.sets} helperText={errors.sets?.message} />
            <TextField size="small" label="Weight (lb)" slotProps={{ inputLabel: { shrink: true } }} type="number" fullWidth {...register('targetLoadLb')}
                       error={!!errors.targetLoadLb} helperText={errors.targetLoadLb?.message} />
            {isNew && TARGET_LABEL[kind] && (
              <TextField size="small" label={TARGET_LABEL[kind]} type="number" fullWidth {...register('target')}
                         error={!!errors.target} helperText={errors.target?.message} />
            )}
          </Stack>
          <Stack direction="row" spacing={1}>
            {timed && (
              <TextField size="small" label="Timer" slotProps={{ inputLabel: { shrink: true } }} placeholder="M:SS" fullWidth {...register('hold')}
                         error={!!errors.hold} helperText={errors.hold?.message ?? 'Blank = the target'} />
            )}
            <TextField size="small" label="Rest" slotProps={{ inputLabel: { shrink: true } }} placeholder="M:SS" fullWidth {...register('rest')}
                       error={!!errors.rest} helperText={errors.rest?.message ?? 'Blank = the default'} />
          </Stack>
          <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
            <Button color="inherit" onClick={onDone}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={isSubmitting || options.isFetching}>Add</Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  )
}
