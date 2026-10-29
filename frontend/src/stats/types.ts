export interface ReportRange {
  from: string
  to: string
}

export interface TrainingReportRow {
  date: string
  workout: string
  tier: string
  location: string | null
  exercisesDone: number
  exercisesPlanned: number
  setsLogged: number
  cleanPercent: number | null
  volumeLb: number
}

export interface TrainingReport {
  title: string
  generatedAt: string
  from: string
  to: string
  rows: TrainingReportRow[]
  sessions: number
  setsLogged: number
  volumeLb: number
}

export interface MuscleBalanceRow {
  muscle: string
  sets: number
  sharePercent: number
  sessions: number
  lastTrained: string | null
}

export interface DirectionBalanceRow {
  direction: string
  sets: number
  sharePercent: number
  sessions: number
}

export interface BalanceReport {
  title: string
  generatedAt: string
  from: string
  to: string
  totalSets: number
  muscles: MuscleBalanceRow[]
  directions: DirectionBalanceRow[]
  pushPullRatio: number | null
}
