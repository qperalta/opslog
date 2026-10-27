import SearchIcon from '@mui/icons-material/Search'
import {
  Alert, Box, Button, Card, CardContent, Chip, InputAdornment, LinearProgress, MenuItem,
  Pagination, Stack, TextField, Typography,
} from '@mui/material'
import { useState } from 'react'
import { labelOf } from '../components/format'
import { PageHeader } from '../components/PageHeader'
import { useDebounced } from '../components/useDebounced'
import { useEquipment, useExerciseKinds, useExercises, useMovementPatterns, useMuscleGroups } from './queries'

const PAGE_SIZE = 10

interface Filters {
  kind: string
  muscleGroupId: string
  movementPatternId: string
  equipmentId: string
}

const NO_FILTERS: Filters = { kind: '', muscleGroupId: '', movementPatternId: '', equipmentId: '' }

export function ExercisesPage() {
  const [text, setText] = useState('')
  const [filters, setFilters] = useState<Filters>(NO_FILTERS)
  const [page, setPage] = useState(0)
  const q = useDebounced(text.trim())

  const kinds = useExerciseKinds()
  const muscles = useMuscleGroups()
  const patterns = useMovementPatterns()
  const equipment = useEquipment()
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
      <PageHeader title="Exercises" />
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

    </>
  )
}
