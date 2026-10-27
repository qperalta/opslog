import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Autocomplete, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from '@mui/material'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useEquipment } from '../catalog/queries'
import { labelOf } from '../components/format'
import { useNotify } from '../components/NotifyContext'
import { useSaveLocation } from './queries'
import type { Location } from './types'

// same limits as LocationRequest
const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(60, 'Name must be 60 characters or fewer'),
  equipmentIds: z.array(z.number()).max(100, 'A location can list at most 100 equipment items'),
})

type LocationValues = z.infer<typeof schema>

interface Props {
  location?: Location
  onClose: () => void
}

/* mount with a key so it starts fresh for each location */
export function LocationFormDialog({ location, onClose }: Props) {
  const equipment = useEquipment()
  const save = useSaveLocation()
  const notify = useNotify()
  const [formError, setFormError] = useState<string | null>(null)
  const { control, register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<LocationValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: location?.name ?? '',
      equipmentIds: location?.equipment.map((item) => item.id) ?? [],
    },
  })

  const onSubmit = async (values: LocationValues) => {
    setFormError(null)
    try {
      const saved = await save.mutateAsync({ id: location?.id, body: values })
      notify(location ? `Updated ${saved.name}` : `Added ${saved.name}`)
      onClose()
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['name', 'equipmentIds']))
    }
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{location ? 'Edit location' : 'New location'}</DialogTitle>
      <DialogContent>
        <Stack component="form" id="location-form" spacing={2} sx={{ pt: 1 }} noValidate onSubmit={handleSubmit(onSubmit)}>
          {formError && <Alert severity="error">{formError}</Alert>}
          <TextField label="Name" {...register('name')} error={!!errors.name} helperText={errors.name?.message} />
          <Controller
            name="equipmentIds"
            control={control}
            render={({ field }) => (
              <Autocomplete
                multiple
                disableCloseOnSelect
                options={equipment.data ?? []}
                groupBy={(item) => labelOf(item.kind)}
                getOptionLabel={(item) => item.name}
                value={(equipment.data ?? []).filter((item) => field.value.includes(item.id))}
                onChange={(_, items) => field.onChange(items.map((item) => item.id))}
                renderInput={(params) => (
                  <TextField {...params} label="Equipment available" error={!!errors.equipmentIds}
                             helperText={errors.equipmentIds?.message ?? 'Sessions here only use this equipment'} />
                )}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button type="submit" form="location-form" variant="contained" disabled={isSubmitting}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  )
}
