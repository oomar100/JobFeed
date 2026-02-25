import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Container,
  Paper,
  Title,
  Text,
  TextInput,
  PasswordInput,
  Button,
  Stack,
  Anchor,
  Group,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { useAuth } from '../context/AuthContext'

export default function SignupPage() {
  const [loading, setLoading] = useState(false)
  const { signup } = useAuth()

  const form = useForm({
    initialValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
    validate: {
      firstName: (value) => (value.length > 0 ? null : 'First name is required'),
      lastName: (value) => (value.length > 0 ? null : 'Last name is required'),
      email: (value) => (/^\S+@\S+$/.test(value) ? null : 'Invalid email'),
      password: (value) =>
        value.length >= 8 ? null : 'Password must be at least 8 characters',
      confirmPassword: (value, values) =>
        value === values.password ? null : 'Passwords do not match',
    },
  })

  const handleSubmit = async (values) => {
    setLoading(true)
    try {
      await signup(values.email, values.password, values.firstName, values.lastName)
    } catch (error) {
      // Error handled in AuthContext
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container size={480} my={100}>
      <Title ta="center" fw={700}>
        Create an account
      </Title>
      <Text c="dimmed" size="sm" ta="center" mt={5}>
        Already have an account?{' '}
        <Anchor component={Link} to="/login" size="sm">
          Sign in
        </Anchor>
      </Text>

      <Paper withBorder shadow="md" p={30} mt={30} radius="md">
        <form onSubmit={form.onSubmit(handleSubmit)}>
          <Stack>
            <Group grow>
              <TextInput
                label="First name"
                placeholder="John"
                required
                {...form.getInputProps('firstName')}
              />
              <TextInput
                label="Last name"
                placeholder="Doe"
                required
                {...form.getInputProps('lastName')}
              />
            </Group>
            <TextInput
              label="Email"
              placeholder="you@example.com"
              required
              {...form.getInputProps('email')}
            />
            <PasswordInput
              label="Password"
              placeholder="At least 8 characters"
              required
              {...form.getInputProps('password')}
            />
            <PasswordInput
              label="Confirm password"
              placeholder="Repeat password"
              required
              {...form.getInputProps('confirmPassword')}
            />
            <Button type="submit" fullWidth loading={loading}>
              Create account
            </Button>
          </Stack>
        </form>
      </Paper>
    </Container>
  )
}
