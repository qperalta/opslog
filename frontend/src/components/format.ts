// STRENGTH -> Strength, FREE_WEIGHT -> Free weight
export function labelOf(kind: string) {
  const text = kind.replaceAll('_', ' ').toLowerCase()
  return text.charAt(0).toUpperCase() + text.slice(1)
}

// "just now", "5 minutes ago", "2 hours ago", "3 days ago"
export function timeAgo(instant: string) {
  const seconds = Math.round((new Date(instant).getTime() - Date.now()) / 1000)
  if(seconds > -60) return 'just now'
  const format = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' })
  if(seconds > -3600) return format.format(Math.round(seconds / 60), 'minute')
  if(seconds > -86400) return format.format(Math.round(seconds / 3600), 'hour')
  return format.format(Math.round(seconds / 86400), 'day')
}

// "Kai Kahale" -> "KK", "Kai" -> "K"
export function initialsOf(name: string | null | undefined) {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean)
  if(parts.length === 0) return '?'
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (parts[0][0] + last).toUpperCase()
}
