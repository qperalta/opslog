import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import SearchIcon from '@mui/icons-material/Search'
import {
  Alert, Box, Button, Card, CardContent, Chip, IconButton, InputAdornment, LinearProgress, MenuItem,
  Pagination, Stack, TextField, Typography,
} from '@mui/material'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { labelOf } from '../components/format'
import { useNotify } from '../components/NotifyContext'
import { PageHeader } from '../components/PageHeader'
import { useDebounced } from '../components/useDebounced'
import { ExerciseFormDialog } from './ExerciseFormDialog'
import {
  useDeleteExercise, useEquipment, useExerciseKinds, useExercises, useMovementPatterns, useMuscleGroups,
} from './queries'
import type { Exercise } from './types'

const PAGE_SIZE = 10

interface Filters {
  kind: string
  muscleGroupId: string
  movementPatternId: string
  equipmentId: string
}

const NO_FILTERS: Filters = { kind: '', muscleGroupId: '', movementPatternId: '', equipmentId: '' }

// undefined = form closed, null = new exercise
type Editing = Exercise | null | undefined

export function ExercisesPage() {
  const [text, setText] = useState('')
  const [filters, setFilters] = useState<Filters>(NO_FILTERS)
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Editing>(undefined)
  const [deleting, setDeleting] = useState<Exercise | null>(null)
  const q = useDebounced(text.trim())
  const notify = useNotify()

  const kinds = useExerciseKinds()
  const muscles = useMuscleGroups()
  const patterns = useMovementPatterns()
  const equipment = useEquipment()
  const remove = useDeleteExercise()
  const results = useExercises({
    q: q || undefined,
    kind: filters.kind || undefined,
    muscleGroupId: Number(filters.muscleGroupId) || undefined,
    movementPatternId: Number(filters.movementPatternId) || undefined,
    equipmentId: Number(filters.equipmentId) || undefined,
    page,
    size: PAGE_SIZE,
  })

  const setFilter = (name: keyof Filters, value: string) => {
    setFilters((current) => ({ ...current, [name]: value }))
    setPage(0)
  }

  const clear = () => {
    setText('')
    setFilters(NO_FILTERS)
    setPage(0)
  }

  const confirmDelete = async () => {
    if (!deleting) return
    try {
      await remove.mutateAsync(deleting.id)
      notify(`Deleted ${deleting.name}`)
    } catch (error) {
      notify(error instanceof ApiError ? error.message : 'Could not delete the exercise', 'error')
    }
    setDeleting(null)
  }

  const filter = (name: keyof Filters, label: string, options: { value: string; label: string }[]) => (
    <TextField select size="small" label={label} value={filters[name]} onChange={(e) => setFilter(name, e.target.value)}>
      <MenuItem value="">All</MenuItem>
      {options.map((option) => (
        <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
      ))}
    </TextField>
  )

  const data = results.data
  return (
    <>
      <PageHeader
        title="Exercises"
        action={<Button startIcon={<AddIcon />} variant="contained" onClick={() => setEditing(null)}>New</Button>}
      />
      <Stack spacing={1.5} sx={{ mb: 2 }}>
        <TextField
          placeholder="Search by name"
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            setPage(0)
          }}
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon /></InputAdornment> } }}
        />
        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
          {filter('kind', 'Kind', (kinds.data ?? []).map((value) => ({ value, label: labelOf(value) })))}
          {filter('muscleGroupId', 'Muscle', (muscles.data ?? []).map((m) => ({ value: String(m.id), label: m.name })))}
          {filter('movementPatternId', 'Pattern', (patterns.data ?? []).map((p) => ({ value: String(p.id), label: p.name })))}
          {filter('equipmentId', 'Equipment', (equipment.data ?? []).map((e) => ({ value: String(e.id), label: e.name })))}
        </Box>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="body2" color="text.secondary">
            {data ? `${data.totalElements} exercise${data.totalElements === 1 ? '' : 's'}` : ' '}
          </Typography>
          <Button size="small" onClick={clear}>Clear filters</Button>
        </Stack>
      </Stack>

      {results.isFetching && <LinearProgress sx={{ mb: 1 }} />}
      {results.isError && <Alert severity="error">{results.error.message}</Alert>}
      {data && data.items.length === 0 && <Alert severity="info">No exercises match these filters.</Alert>}

      <Stack spacing={1.5}>
        {data?.items.map((exercise) => (
          <Card key={exercise.id} variant="outlined">
            <CardContent sx={{ '&:last-child': { pb: 2 } }}>
              <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
                <Box>
                  <Typography sx={{ fontWeight: 600 }}>{exercise.name}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {exercise.movementPattern.name} · {exercise.primaryMuscle.name} · {exercise.target}
                  </Typography>
                </Box>
                {exercise.custom && (
                  <Stack direction="row">
                    <IconButton size="small" aria-label={`Edit ${exercise.name}`} onClick={() => setEditing(exercise)}>
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" aria-label={`Delete ${exercise.name}`} onClick={() => setDeleting(exercise)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                )}
              </Stack>
              <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                <Chip size="small" color="primary" label={labelOf(exercise.kind)} />
                {exercise.custom && <Chip size="small" color="secondary" label="Custom" />}
                {exercise.equipment.map((item) => (
                  <Chip key={item.id} size="small" variant="outlined" label={item.name} />
                ))}
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {data && data.totalPages > 1 && (
        <Pagination
          sx={{ mt: 2, display: 'flex', justifyContent: 'center' }}
          count={data.totalPages}
          page={page + 1}
          onChange={(_, value) => setPage(value - 1)}
        />
      )}

      {editing !== undefined && (
        <ExerciseFormDialog key={editing?.id ?? 'new'} exercise={editing ?? undefined} onClose={() => setEditing(undefined)} />
      )}
      <ConfirmDialog
        open={deleting !== null}
        title="Delete exercise?"
        message={`${deleting?.name ?? ''} will be removed from your catalog.`}
        busy={remove.isPending}
        onConfirm={confirmDelete}
        onClose={() => setDeleting(null)}
      />
    </>
  )
}
