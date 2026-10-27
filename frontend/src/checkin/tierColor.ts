// display color per tier kind; unknown kinds fall back to info
const COLORS: Record<string, 'success' | 'warning' | 'error' | 'info'> = {
  FULL: 'success',
  REDUCED: 'warning',
  RECOVERY: 'error',
}

export const tierColor = (kind: string) => COLORS[kind] ?? 'info'
