import { useEffect } from 'react'

const INTERVAL_MS = 4 * 60 * 1000

const ping = () =>
  fetch('/actuator/health').catch(() => {})

export function useKeepAwake() {
  useEffect(() => {
    let timer: number | undefined
    const start = () => timer = window.setInterval(ping, INTERVAL_MS)
    const stop = () => window.clearInterval(timer)
    const onVisibilityChange = () => {
      stop()
      if(document.visibilityState === 'visible') {
        ping()
        start()
      }
    }

    if(document.visibilityState === 'visible') start()
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      stop()
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [])
}