import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  Accordion, AccordionDetails, AccordionSummary, Alert, Box, Button, Chip, IconButton, LinearProgress, Stack, Typography,
} from '@mui/material'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useNotify } from '../components/NotifyContext'
import { PageHeader } from '../components/PageHeader'
import { LocationFormDialog } from './LocationFormDialog'
import { useDeleteLocation, useLocations } from './queries'
import type { Location } from './types'

// undefined = form closed, null = new location
type Editing = Location | null | undefined

export function LocationsPage() {
  const locations = useLocations()
  const remove = useDeleteLocation()
  const notify = useNotify()
  const [editing, setEditing] = useState<Editing>(undefined)
  const [deleting, setDeleting] = useState<Location | null>(null)

  const confirmDelete = async () => {
    if (!deleting) return
    try {
      await remove.mutateAsync(deleting.id)
      notify(`Deleted ${deleting.name}`)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Could not delete the location', 'error')
    }
    setDeleting(null)
  }

  return (
    <>
      <PageHeader
        title="Locations"
        action={<Button startIcon={<AddIcon />} variant="contained" onClick={() => setEditing(null)}>New</Button>}
      />
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Kaimana gyms are shared and read-only. Add your own, such as a home gym, with the equipment you have there.
      </Typography>

      {locations.isPending && <LinearProgress />}
      {locations.isError && <Alert severity="error">{locations.error.message}</Alert>}

      {locations.data?.map((location) => (
        <Accordion key={location.id} disableGutters>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Box sx={{ flexGrow: 1 }}>
              <Typography sx={{ fontWeight: 600 }}>{location.name}</Typography>
              <Typography variant="body2" color="text.secondary">
                {location.equipment.length} equipment item{location.equipment.length === 1 ? '' : 's'}
              </Typography>
            </Box>
            <Chip
              size="small"
              sx={{ alignSelf: 'center', mr: 1 }}
              color={location.custom ? 'secondary' : 'default'}
              label={location.custom ? 'Yours' : 'Shared'}
            />
          </AccordionSummary>
          <AccordionDetails>
            <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.5 }}>
              {location.equipment.map((item) => (
                <Chip key={item.id} size="small" variant="outlined" label={item.name} />
              ))}
              {location.equipment.length === 0 && (
                <Typography variant="body2" color="text.secondary">No equipment listed.</Typography>
              )}
            </Stack>
            {location.custom && (
              <Stack direction="row" sx={{ justifyContent: 'flex-end', mt: 1 }}>
                <IconButton aria-label={`Edit ${location.name}`} onClick={() => setEditing(location)}>
                  <EditIcon />
                </IconButton>
                <IconButton aria-label={`Delete ${location.name}`} onClick={() => setDeleting(location)}>
                  <DeleteIcon />
                </IconButton>
              </Stack>
            )}
          </AccordionDetails>
        </Accordion>
      ))}

      {editing !== undefined && (
        <LocationFormDialog key={editing?.id ?? 'new'} location={editing ?? undefined} onClose={() => setEditing(undefined)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        title="Delete location?"
        message={`${deleting?.name ?? ''} will be removed. Past check-ins keep their data but no longer show the location.`}
        busy={remove.isPending}
        onConfirm={confirmDelete}
        onClose={() => setDeleting(null)}
      />
    </>
  )
}
