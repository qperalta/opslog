/* only thought about having a keep awake after creating the WakeNotice in login
 * so now we actually create a shared hook so it can be used globally.
 */

import { useEffect, useState } from 'react'

export function useSlow(pending: boolean, delayMs = 3000) {
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    if(!pending) return
    const timer = setTimeout(() => setSlow(true), delayMs)
    return () => {
      clearTimeout(timer)
      setSlow(false)
    }
  }, [pending, delayMs])
  return pending && slow
}