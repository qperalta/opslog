import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import {
  Alert, Autocomplete, Button, Card, LinearProgress, MenuItem, Stack, Table, TableBody, TableCell, TableContainer,
  TableHead, TablePagination, TableRow, TextField, Typography,
} from '@mui/material'
import { useState } from 'react'
import { formatDay } from '../checkin/dates'
import { useExercises, useMuscleGroups } from '../catalog/queries'
import { describeSet } from '../today/setFormat'
import { useHistory } from './queries'
import type { HistoryRow } from './types'

interface Filters {
  exerciseId: number | null
  muscleGroupId: string
  from: string
  to: string
}

const NO_FILTERS: Filters = { exerciseId: null, muscleGroupId: '', from: '', to: '' }

const describe = (row: HistoryRow) =>
  describeSet({ ...row, loggedAt: '' }, row.kind)

export function HistorySection() {
  const [filters, setFilters] = useState<Filters>(NO_FILTERS)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)

  // every visible exercise fits in one page of 100
  const exercises = useExercises({ page: 0, size: 100 })
  const muscles = useMuscleGroups()

  // checked here too so the request is never sent with a backwards range
  const badRange = !!filters.from && !!filters.to && filters.from > filters.to
  const history = useHistory({
    exerciseId: filters.exerciseId ?? undefined,
    muscleGroupId: Number(filters.muscleGroupId) || undefined,
    from: filters.from || undefined,
    to: filters.to || undefined,
    page,
    size,
  }, !badRange)

  const setFilter = <K extends keyof Filters>(name: K, value: Filters[K]) => {
    setFilters((current) => ({ ...current, [name]: value }))
    setPage(0)
  }

  const options = exercises.data?.items ?? []
  const rows = history.data?.items ?? []

  return (
    <>
      <Stack spacing={1.5} sx={{ mb: 2 }}>
        <Autocomplete
          options={options}
          getOptionLabel={(item) => item.name}
          value={options.find((item) => item.id === filters.exerciseId) ?? null}
          onChange={(_, item) => setFilter('exerciseId', item?.id ?? null)}
          renderInput={(params) => <TextField {...params} label="Exercise" size="small" />}
        />
        <TextField select label="Primary muscle" size="small" value={filters.muscleGroupId}
                   onChange={(event) => setFilter('muscleGroupId', event.target.value)}>
          <MenuItem value="">Any muscle</MenuItem>
          {(muscles.data ?? []).map((item) => <MenuItem key={item.id} value={String(item.id)}>{item.name}</MenuItem>)}
        </TextField>
        <Stack direction="row" spacing={1.5}>
          <TextField type="date" label="From" size="small" fullWidth value={filters.from}
                     onChange={(event) => setFilter('from', event.target.value)}
                     slotProps={{ inputLabel: { shrink: true } }} error={badRange} />
          <TextField type="date" label="To" size="small" fullWidth value={filters.to}
                     onChange={(event) => setFilter('to', event.target.value)}
                     slotProps={{ inputLabel: { shrink: true } }} error={badRange}
                     helperText={badRange ? 'Must be on or after From' : undefined} />
        </Stack>
        <Button size="small" onClick={() => { setFilters(NO_FILTERS); setPage(0) }} sx={{ alignSelf: 'flex-start' }}>
          Clear filters
        </Button>
      </Stack>

      {history.isFetching && <LinearProgress />}
      {history.isError && <Alert severity="error">{history.error.message}</Alert>}

      {history.data && rows.length === 0 && (
        <Alert severity="info">No logged sets match these filters.</Alert>
      )}

      {rows.length > 0 && (
        <Card variant="outlined">
          <TableContainer>
            <Table size="small" aria-label="Logged sets">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Exercise</TableCell>
                  <TableCell>Result</TableCell>
                  <TableCell align="right">Set</TableCell>
                  <TableCell>Muscle</TableCell>
                  <TableCell>Workout</TableCell>
                  <TableCell>Location</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={`${row.date}-${row.exerciseId}-${row.setNumber}`}>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDay(row.date, { month: 'short', day: 'numeric' })}</TableCell>
                    <TableCell sx={{ minWidth: 110 }}>{row.exercise}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>
                      <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                        <span>{describe(row)}</span>
                        {!row.clean && <WarningAmberIcon fontSize="small" color="warning" titleAccess="Technique not clean" />}
                      </Stack>
                    </TableCell>
                    <TableCell align="right">{row.setNumber}</TableCell>
                    <TableCell>{row.primaryMuscle}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.workout ?? '—'}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{row.location ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={history.data?.totalElements ?? 0}
            page={page}
            rowsPerPage={size}
            rowsPerPageOptions={[10, 20, 50]}
            onPageChange={(_, next) => setPage(next)}
            onRowsPerPageChange={(event) => { setSize(Number(event.target.value)); setPage(0) }}
          />
        </Card>
      )}

      {history.data && (
        <Typography variant="caption" color="text.secondary" component="p" sx={{ mt: 1 }}>
          {history.data.totalElements} logged {history.data.totalElements === 1 ? 'set' : 'sets'}
        </Typography>
      )}
    </>
  )
}
