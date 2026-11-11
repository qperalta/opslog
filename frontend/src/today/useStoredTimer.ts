import { useCallback, useState } from 'react'

// a timer that ended this long ago is dropped instead of restored
const STALE_MS = 10 * 60 * 1000

interface Stored {
  date: string
  endsAt: number
}

function read(key: string, date: string) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    const stored = JSON.parse(raw) as Stored
    return stored.date === date && stored.endsAt + STALE_MS > Date.now() ? stored.endsAt : null
  } catch {
    return null
  }
}

/* an end time kept in localStorage, so a reload or a killed tab picks the timer back up */
export function useStoredTimer(key: string, date: string) {
  const [endsAt, setEndsAt] = useState(() => read(key, date))

  // stable, so effects that list it don't restart every render
  const set = useCallback((value: number | null) => {
    try {
      if (value == null) localStorage.removeItem(key)
      else localStorage.setItem(key, JSON.stringify({ date, endsAt: value } satisfies Stored))
    } catch {
      // blocked storage: the timer still runs, it just won't survive a reload
    }
    setEndsAt(value)
  }, [key, date])

  return [endsAt, set] as const
}
