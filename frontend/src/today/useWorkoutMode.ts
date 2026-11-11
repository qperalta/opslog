import { useState } from 'react'

// holds the date of the workout in progress, so a reload stays in workout mode
const KEY = 'opslog.workout'

function read() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

function write(date: string | null) {
  try {
    if (date) localStorage.setItem(KEY, date)
    else localStorage.removeItem(KEY)
  } catch {
    // private mode or blocked storage: workout mode just won't survive a reload
  }
}

export function useWorkoutMode(date: string) {
  const [active, setActive] = useState(() => read() === date)

  const set = (on: boolean) => {
    write(on ? date : null)
    setActive(on)
  }

  return { active, start: () => set(true), finish: () => set(false) }
}
