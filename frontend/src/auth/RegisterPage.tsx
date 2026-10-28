import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router'
import { z } from 'zod'
import { api } from '../api/client'
import { applyServerErrors } from '../api/formErrors'
import { useNotify } from '../components/NotifyContext'
import { AuthCard } from './AuthCard'
import { useAuth } from './AuthContext'

// same rules as RegisterRequest on the server
const schema = z
  .object({
    email: z.string().trim().min(1, 'Email is required').max(254, 'Email must be 254 characters or fewer')
      .pipe(z.email('Enter a valid email address')),
    displayName: z.string().trim().min(1, 'Display name is required').max(50, 'Display name must be 50 characters or fewer'),
    password: z.string().min(8, 'Password must be 8 to 72 characters long').max(72, 'Password must be 8 to 72 characters long'),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })

type RegisterValues = z.infer<typeof schema>

export function RegisterPage() {
  const { user, login } = useAuth()
  const notify = useNotify()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<RegisterValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', displayName: '', password: '', confirmPassword: '' },
  })

  if (user) return <Navigate to="/today" replace />

  const onSubmit = async ({ email, displayName, password }: RegisterValues) => {
    setFormError(null)
    try {
      await api('/api/auth/register', { method: 'POST', body: { email, displayName, password } })
      await login(email, password)
      notify(`Welcome, ${displayName}! Your account was created.`)
      navigate('/today', { replace: true })
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['email', 'displayName', 'password']))
    }
  }

  return (
    <AuthCard title="Create an account">
      <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit(onSubmit)}>
        {formError && <Alert severity="error">{formError}</Alert>}
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <TextField
          label="Display name"
          autoComplete="nickname"
          {...register('displayName')}
          error={!!errors.displayName}
          helperText={errors.displayName?.message}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message ?? 'At least 8 characters'}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
        />
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          Create account
        </Button>
        <Typography variant="body2">
          Already have an account?{' '}
          <Link component={RouterLink} to="/login">
            Sign in
          </Link>
        </Typography>
      </Stack>
    </AuthCard>
  )
}
