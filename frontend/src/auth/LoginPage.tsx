import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Button, Link, Stack, TextField, Typography } from '@mui/material'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { applyServerErrors } from '../api/formErrors'
import { useNotify } from '../components/NotifyContext'
import { AuthCard } from './AuthCard'
import { useAuth } from './AuthContext'

const schema = z.object({
  email: z.string().trim().min(1, 'Email is required'),
  password: z.string().min(1, 'Password is required'),
})

type LoginValues = z.infer<typeof schema>

export function LoginPage() {
  const { user, login } = useAuth()
  const notify = useNotify()
  const navigate = useNavigate()
  const location = useLocation()
  const [formError, setFormError] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  const from = (location.state as { from?: string } | null)?.from ?? '/today'
  if (user) return <Navigate to={from} replace />

  const onSubmit = async (values: LoginValues) => {
    setFormError(null)
    try {
      await login(values.email, values.password)
      notify('Signed in')
      navigate(from, { replace: true })
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['email', 'password']))
    }
  }

  return (
    <AuthCard title="Sign in">
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
          label="Password"
          type="password"
          autoComplete="current-password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
        />
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          Sign in
        </Button>
        <Typography variant="body2">
          New here?{' '}
          <Link component={RouterLink} to="/register">
            Create an account
          </Link>
        </Typography>
      </Stack>
    </AuthCard>
  )
}
