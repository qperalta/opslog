import type { ExerciseRequest } from '../catalog/types'
import type { Lookup } from '../api/types'
import type { Actor } from '../planner/types'

export type SessionStatus = 'READY' | 'REST' | 'NEEDS_CHECKIN'

export interface SessionExercise {
  id: number
  position: number
  exerciseId: number
  name: string
  kind: string
  target: string
  sets: number
  targetLoadLb: number | null
  mainLift: boolean
  substitutedFor: Lookup | null
  reason: string | null
  skipped: boolean
  adHoc: boolean
  logged: LoggedSet[]
}

// strength sets log reps, timed and cardio sets log seconds
export interface LoggedSet {
  setNumber: number
  reps: number | null
  durationSeconds: number | null
  loadLb: number | null
  clean: boolean
  loggedAt: string
}

export interface LoggedSetBody {
  reps: number | null
  durationSeconds: number | null
  loadLb: number | null
  clean: boolean
}

// mirrors InsertExerciseRequest: exerciseId for one from the list, or newExercise for a name that isn't
export interface InsertExerciseBody {
  position: number
  exerciseId: number | null
  newExercise: ExerciseRequest | null
  sets: number
  targetLoadLb: number | null
}

export interface Session {
  date: string
  status: SessionStatus
  workout: Lookup | null
  tier: Lookup | null
  tierKind: string | null
  location: Lookup | null
  source: Actor | null
  notes: string[]
  exercises: SessionExercise[]
  createdAt: string | null
}
