import type { Lookup } from '../api/types'

export type PlanMode = 'AUTO' | 'TRAINING' | 'REST'
export type Actor = 'USER' | 'RULES' | 'AGENT'

export interface WorkoutItem {
  id: number
  position: number
  exercise: Lookup
  primaryMuscle: Lookup
  target: string
  sets: number
  mainLift: boolean
}

export interface Workout {
  id: number
  position: number
  name: string
  exercises: WorkoutItem[]
}

export interface ProgramTemplate {
  id: number
  name: string
  description: string
  trainingDays: string[]
  workouts: Workout[]
}

export interface UserProgram {
  template: Lookup
  location: Lookup | null
  startedOn: string
}

export interface UserProgramRequest {
  templateId: number
  locationId: number | null
}

export interface PlanDay {
  date: string
  dayOfWeek: string
  mode: PlanMode
  workout: Lookup | null
  location: Lookup | null
  locked: boolean
  updatedBy: Actor
  updatedAt: string
}

export interface Week {
  start: string
  end: string
  program: UserProgram
  days: PlanDay[]
}

export interface PlanDayRequest {
  mode: PlanMode
  workoutId: number | null
  locationId: number | null
  locked: boolean
}
