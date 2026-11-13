import type { LoggedSet, SessionExercise } from './types'

// cardio is entered and shown in minutes, stored in seconds
export const isCardio = (kind: string) => kind === 'CARDIO'
export const isStrength = (kind: string) => kind === 'STRENGTH'

export const measureLabel = (kind: string) => isStrength(kind) ? 'Reps' : isCardio(kind) ? 'Min' : 'Sec'

export function describeSet(set: LoggedSet, kind: string) {
  const measure = isStrength(kind)
    ? `${set.reps} reps`
    : isCardio(kind) ? `${Math.round((set.durationSeconds ?? 0) / 60)} min` : `${set.durationSeconds} s`
  return set.loadLb != null ? `${measure} @ ${set.loadLb} lb` : measure
}

// main lifts get the longest rest; the member's own rest time wins
export const defaultRest = (item: SessionExercise) => item.mainLift ? 180 : isStrength(item.kind) ? 90 : 60
export const restSeconds = (item: SessionExercise) => item.restSeconds ?? defaultRest(item)

export const clock = (seconds: number) => `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

/* "8-12 reps" -> "8-12", "75 s" -> "75"; only a single number can stand in for an empty field */
export const targetText = (item: SessionExercise) => item.target.split(' ')[0]
export const fixedTarget = (item: SessionExercise) => /^\d+$/.test(targetText(item)) ? targetText(item) : ''

// timed exercises only: the member's own hold time, then the exercise's target
export const holdSeconds = (item: SessionExercise) =>
  item.kind === 'TIMED' ? item.holdSeconds ?? Number(fixedTarget(item) || 0) : 0

// "1:15" or "75" -> 75; null when it isn't a time
export function parseClock(text: string) {
  const value = text.trim()
  const minutes = /^(\d{1,2}):([0-5]\d)$/.exec(value)
  if (minutes) return Number(minutes[1]) * 60 + Number(minutes[2])
  return /^\d{1,4}$/.test(value) ? Number(value) : null
}
