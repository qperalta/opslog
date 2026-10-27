import type { Lookup } from '../api/types'

export interface Tier {
  id: number
  kind: string
  name: string
  description: string
  minSleep: number
  maxSoreness: number
}

export interface CheckIn {
  id: number
  date: string
  sleepQuality: number
  soreness: number
  location: Lookup | null
  tier: Tier
  updatedAt: string
}

export interface CheckInRequest {
  sleepQuality: number
  soreness: number
  locationId: number
}
