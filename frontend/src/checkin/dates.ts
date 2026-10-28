// the user's calendar date, not UTC, as YYYY-MM-DD
export const todayLocal = () => new Date().toLocaleDateString('en-CA')

/* parses YYYY-MM-DD as a local date so it does not shift a day in Hawaii */
export function formatDay(date: string, options: Intl.DateTimeFormatOptions = { weekday: 'short', month: 'short', day: 'numeric' }) {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day).toLocaleDateString(undefined, options)
}

/* YYYY-MM-DD plus n days, in local time */
export function addDays(date: string, days: number) {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day + days).toLocaleDateString('en-CA')
}
