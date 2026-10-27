import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from './client'

/* puts server field errors under the matching inputs; returns the message for the form-level alert */
export function applyServerErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): string {
  if (!(error instanceof ApiError)) return 'Something went wrong. Please try again.'
  let unmatched = false
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    if ((fields as readonly string[]).includes(field)) setError(field as Path<T>, { type: 'server', message })
    else unmatched = true
  }
  const matched = Object.keys(error.fieldErrors).length > 0 && !unmatched
  return matched ? 'Please fix the highlighted fields.' : error.message
}
