import type { Lookup } from '../api/types'
import type { Actor } from '../planner/types'

export type SessionStatus = 'READY' | 'REST' | 'NEEDS_CHECKIN'

export interface SessionExercise {
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
