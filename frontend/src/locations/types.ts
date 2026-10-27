import type { Lookup } from '../api/types'

export interface Location {
  id: number
  name: string
  custom: boolean
  equipment: Lookup[]
}

export interface LocationRequest {
  name: string
  equipmentIds: number[]
}
