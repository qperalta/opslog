import { useEffect } from 'react'

/* keeps the screen on while enabled; the browser drops the lock when the page is hidden, so it is taken again on return */
export function useWakeLock(enabled: boolean) {
  useEffect(() => {
    if (!enabled || !('wakeLock' in navigator)) return
    let lock: WakeLockSentinel | null = null
    let stopped = false

    const request = async () => {
      try {
        const next = await navigator.wakeLock.request('screen')
        if (stopped) await next.release()
        else lock = next
      } catch {
        // refused, e.g. low battery mode; the workout still works
      }
    }
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') void request()
    }

    void request()
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      stopped = true
      document.removeEventListener('visibilitychange', onVisibilityChange)
      void lock?.release()
    }
  }, [enabled])
}
