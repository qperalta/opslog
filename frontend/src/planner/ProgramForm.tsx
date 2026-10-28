import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, MenuItem, Stack, TextField } from '@mui/material'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useNotify } from '../components/NotifyContext'
import { useLocations } from '../locations/queries'
import { useChooseProgram, usePrograms } from './queries'
import type { UserProgram } from './types'

const schema = z.object({
  templateId: z.number({ error: 'Program is required' }),
  // 0 = no default location
  locationId: z.number(),
})

type ProgramValues = z.infer<typeof schema>

interface Props {
  current: UserProgram | null
  onDone?: () => void
}

export function ProgramForm({ current, onDone }: Props) {
  const programs = usePrograms()
  const locations = useLocations()
  const choose = useChooseProgram()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<ProgramValues>({
    resolver: zodResolver(schema),
    defaultValues: { templateId: current?.template.id, locationId: current?.location?.id ?? 0 },
  })

  const onSubmit = async (values: ProgramValues) => {
    setFormError(null)
    try {
      const saved = await choose.mutateAsync({ templateId: values.templateId, locationId: values.locationId || null })
      notify(`Program set: ${saved.template.name}`)
      onDone?.()
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['templateId', 'locationId']))
    }
  }

  return (
    <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit(onSubmit)}>
      {formError && <Alert severity="error">{formError}</Alert>}
      <Controller
        name="templateId"
        control={control}
        render={({ field }) => (
          <TextField
            select
            label="Program"
            value={field.value ?? ''}
            onChange={(e) => field.onChange(Number(e.target.value))}
            onBlur={field.onBlur}
            inputRef={field.ref}
            error={!!errors.templateId}
            helperText={errors.templateId?.message
              ?? programs.data?.find((program) => program.id === field.value)?.description}
          >
            {(programs.data ?? []).map((program) => (
              <MenuItem key={program.id} value={program.id}>
                {program.name}
              </MenuItem>
            ))}
          </TextField>
        )}
      />
      <Controller
        name="locationId"
        control={control}
        render={({ field }) => (
          <TextField
            select
            label="Usual location"
            value={field.value}
            onChange={(e) => field.onChange(Number(e.target.value))}
            onBlur={field.onBlur}
            inputRef={field.ref}
            error={!!errors.locationId}
            helperText={errors.locationId?.message ?? 'Days use this unless you pick another one'}
          >
            <MenuItem value={0}>None</MenuItem>
            {(locations.data ?? []).map((location) => (
              <MenuItem key={location.id} value={location.id}>
                {location.name}
              </MenuItem>
            ))}
          </TextField>
        )}
      />
      <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
        {onDone && <Button onClick={onDone}>Cancel</Button>}
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          {current ? 'Save program' : 'Start program'}
        </Button>
      </Stack>
    </Stack>
  )
}
