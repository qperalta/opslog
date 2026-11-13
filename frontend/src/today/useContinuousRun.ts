import { useCallback, useEffect, useRef, useState } from 'react'
import { useNotify } from '../components/NotifyContext'
import { useSaveSet } from './queries'
import type { SessionExercise } from './types'

// one phase of a continuous run; each end time follows the last, so a locked phone catches up when it wakes
export interface Run {
  date: string
  phase: 'hold' | 'rest'
  endsAt: number
  hold: number
  rest: number
  left: number
  total: number
  load: number | null
}

// a run left this far behind (tab closed, phone off for long) is dropped instead of logging every missed set
const STALE_MS = 10 * 60 * 1000

const keyOf = (sessionExerciseId: number) => `opslog.run.${sessionExerciseId}`

function read(key: string, date: string): Run | null {
  try {
    const raw = localStorage.getItem(key)
    const run = raw ? JSON.parse(raw) as Run : null
    return run?.date === date && run.endsAt + STALE_MS > Date.now() ? run : null
  } catch {
    return null
  }
}

function write(key: string, run: Run | null) {
  try {
    if (run) localStorage.setItem(key, JSON.stringify(run))
    else localStorage.removeItem(key)
  } catch {
    // blocked storage: the run still works, it just won't survive a reload
  }
}

// Finish workout ends every run, so nothing keeps logging after the workout
export function clearRuns(sessionExerciseIds: number[]) {
  sessionExerciseIds.forEach((id) => write(keyOf(id), null))
}

/* hold, log the set, rest, repeat until the sets are done; Stop logs a hold cut short */
export function useContinuousRun(date: string, item: SessionExercise) {
  const key = keyOf(item.id)
  const { mutateAsync: saveSet } = useSaveSet()
  const notify = useNotify()
  const [run, setRun] = useState(() => read(key, date))
  const [now, setNow] = useState(() => Date.now())
  const runRef = useRef(run)
  const busy = useRef(false)
  // the position can move if an exercise is inserted above during the run
  const itemRef = useRef(item)
  useEffect(() => { itemRef.current = item })

  const persist = useCallback((next: Run | null) => {
    runRef.current = next
    write(key, next)
    setRun(next)
  }, [key])

  const logHold = useCallback((seconds: number, load: number | null) =>
    saveSet({
      target: { date, position: itemRef.current.position },
      body: { reps: null, durationSeconds: seconds, loadLb: load, clean: true },
    }), [saveSet, date])

  // one step per tick; a phase that ended while the screen was off is handled on the next ticks
  const step = useCallback(() => {
    const current = runRef.current
    if (!current || busy.current || Date.now() < current.endsAt) return
    if (current.phase === 'rest') {
      persist({ ...current, phase: 'hold', endsAt: current.endsAt + current.hold * 1000 })
      return
    }
    busy.current = true
    logHold(current.hold, current.load)
      .then(() => {
        const left = current.left - 1
        if (left <= 0) {
          persist(null)
          navigator.vibrate?.([300, 150, 300])
          notify(`${itemRef.current.name} done`)
          return
        }
        navigator.vibrate?.(300)
        persist(current.rest > 0
          ? { ...current, left, phase: 'rest', endsAt: current.endsAt + current.rest * 1000 }
          : { ...current, left, phase: 'hold', endsAt: current.endsAt + current.hold * 1000 })
      })
      .catch((failure: Error) => {
        persist(null)
        notify(failure.message, 'error')
      })
      .finally(() => { busy.current = false })
  }, [persist, logHold, notify])

  const active = run != null
  useEffect(() => {
    if (!active) return
    const id = window.setInterval(() => {
      setNow(Date.now())
      step()
    }, 250)
    return () => window.clearInterval(id)
  }, [active, step])

  // sets = how many to run now; done = sets already logged, for "Set 3 of 4"
  const start = (hold: number, rest: number, sets: number, done: number, load: number | null) => {
    if (hold <= 0 || sets <= 0) return
    setNow(Date.now())
    persist({ date, phase: 'hold', endsAt: Date.now() + hold * 1000, hold, rest, left: sets, total: done + sets, load })
  }

  // a hold stopped early logs the seconds held; stopping during rest logs nothing
  const stop = () => {
    const current = runRef.current
    if (!current) return
    persist(null)
    if (current.phase !== 'hold') return
    const held = Math.min(current.hold, Math.floor((Date.now() - (current.endsAt - current.hold * 1000)) / 1000))
    if (held >= 1) {
      logHold(held, current.load)
        .then(() => notify(`Logged ${held} s`))
        .catch((failure: Error) => notify(failure.message, 'error'))
    }
  }

  const secondsLeft = run ? Math.max(0, Math.ceil((run.endsAt - now) / 1000)) : 0
  return { run, secondsLeft, start, stop }
}
