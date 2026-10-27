// STRENGTH -> Strength, FREE_WEIGHT -> Free weight
export function labelOf(kind: string) {
  const text = kind.replaceAll('_', ' ').toLowerCase()
  return text.charAt(0).toUpperCase() + text.slice(1)
}
