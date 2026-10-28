// mirrors HistoryRow and HistorySearch
export interface HistoryRow {
  date: string
  workout: string | null
  location: string | null
  exerciseId: number
  exercise: string
  kind: string
  primaryMuscle: string
  setNumber: number
  reps: number | null
  durationSeconds: number | null
  loadLb: number | null
  clean: boolean
}

export interface HistorySearch {
  exerciseId?: number
  muscleGroupId?: number
  from?: string
  to?: string
  page: number
  size: number
}
