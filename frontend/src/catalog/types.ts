import type { Lookup } from '../api/types'

export interface Equipment {
  id: number
  name: string
  kind: string
  loadIncrementLb: number | null
  maxLoadLb: number | null
  progressionHint: string
}

export interface Exercise {
  id: number
  name: string
  kind: string
  movementPattern: Lookup
  primaryMuscle: Lookup
  target: string
  repMin: number | null
  repMax: number | null
  targetSeconds: number | null
  durationMinutes: number | null
  custom: boolean
  equipment: Lookup[]
}

export interface ExerciseRequest {
  kind: string
  name: string
  movementPatternId: number
  primaryMuscleId: number
  repMin?: number
  repMax?: number
  targetSeconds?: number
  durationMinutes?: number
  equipmentIds: number[]
}

export interface ExerciseSearch {
  q?: string
  muscleGroupId?: number
  movementPatternId?: number
  equipmentId?: number
  kind?: string
  page: number
  size: number
}
