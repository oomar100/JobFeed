import { useState } from 'react'
import {
  Modal,
  TextInput,
  NumberInput,
  Button,
  Stack,
  TagsInput,
  Textarea,
  Switch,
  Group,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { notifications } from '@mantine/notifications'
import { taskService } from '../../services/taskService'

export default function CreateTaskModal({ opened, onClose, onSuccess }) {
  const [loading, setLoading] = useState(false)

  const form = useForm({
    initialValues: {
      jobTitle: '',
      location: '',
      numJobs: 20,
      age: 0,
      skills: [],
      yearsOfExperience: null,
      thingsToAvoid: [],
      additionalPreferences: '',
      recurring: false,
      intervalHours: null,
    },
    validate: {
      jobTitle: (value) => (value.length > 0 ? null : 'Job title is required'),
      location: (value) => (value.length > 0 ? null : 'Location is required'),
      numJobs: (value) => (value >= 1 && value <= 100 ? null : 'Must be between 1 and 100'),
    },
  })

  const handleSubmit = async (values) => {
    setLoading(true)
    try {
      const payload = {
        ...values,
        intervalHours: values.recurring ? values.intervalHours : null,
      }
      await taskService.create(payload)
      notifications.show({
        title: 'Task created',
        message: 'Your scraping task has been created',
        color: 'green',
      })
      form.reset()
      onSuccess()
    } catch (error) {
      notifications.show({
        title: 'Failed to create task',
        message: error.response?.data?.error || 'Something went wrong',
        color: 'red',
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal opened={opened} onClose={onClose} title="Create New Task" size="lg">
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack gap="md">
          <TextInput
            label="Job Title"
            placeholder="e.g., Software Engineer"
            required
            {...form.getInputProps('jobTitle')}
          />

          <TextInput
            label="Location"
            placeholder="e.g., New York, NY or Remote"
            required
            {...form.getInputProps('location')}
          />

          <NumberInput
            label="Number of Jobs"
            placeholder="How many jobs to scrape"
            min={1}
            max={100}
            required
            {...form.getInputProps('numJobs')}
          />

            <NumberInput
            label="Job age"
            placeholder="How long was the job posted '1' for jobs posted within the last 24 hours "
            min={1}
            max={7}
            required
            {...form.getInputProps('age')}
          />

          <TagsInput
            label="Skills"
            placeholder="Press Enter to add skills"
            description="Add relevant skills to match against job descriptions"
            {...form.getInputProps('skills')}
          />

          <NumberInput
            label="Years of Experience"
            placeholder="Your years of experience"
            min={0}
            max={50}
            {...form.getInputProps('yearsOfExperience')}
          />

          <TagsInput
            label="Things to Avoid"
            placeholder="Press Enter to add"
            description="Keywords that should lower the job's score (e.g., Clearance Required)"
            {...form.getInputProps('thingsToAvoid')}
          />

          <Textarea
            label="Additional Preferences"
            placeholder="Any other preferences for ranking jobs..."
            minRows={2}
            {...form.getInputProps('additionalPreferences')}
          />

          <Switch
            label="Recurring task"
            description="Automatically run this task on a schedule"
            {...form.getInputProps('recurring', { type: 'checkbox' })}
          />

          {form.values.recurring && (
            <NumberInput
              label="Interval (hours)"
              placeholder="Run every X hours"
              min={1}
              max={168}
              {...form.getInputProps('intervalHours')}
            />
          )}

          <Group justify="flex-end" mt="md">
            <Button variant="subtle" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={loading}>
              Create Task
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
