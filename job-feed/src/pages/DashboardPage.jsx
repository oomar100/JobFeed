import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Title,
  Text,
  Button,
  Group,
  Stack,
  Card,
  Badge,
  Menu,
  ActionIcon,
  Collapse,
  SimpleGrid,
  Loader,
  Center,
  Paper,
} from '@mantine/core'
import {
  IconPlus,
  IconDotsVertical,
  IconTrash,
  IconPlayerPause,
  IconPlayerPlay,
  IconChevronDown,
  IconChevronUp,
  IconRefresh,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { taskService } from '../services/taskService'
import CreateTaskModal from '../components/tasks/CreateTaskModal'

const statusColors = {
  SCHEDULED: 'blue',
  SCRAPING: 'yellow',
  RANKING: 'orange',
  COMPLETED: 'green',
  FAILED: 'red',
  PAUSED: 'gray',
}

function TaskCard({ task, onRefresh }) {
  const [expanded, setExpanded] = useState(false)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleAction = async (action) => {
    setLoading(true)
    try {
      switch (action) {
        case 'run':
          await taskService.run(task.id)
          notifications.show({ title: 'Task started', color: 'green' })
          break
        case 'pause':
          await taskService.pause(task.id)
          notifications.show({ title: 'Task paused', color: 'yellow' })
          break
        case 'resume':
          await taskService.resume(task.id)
          notifications.show({ title: 'Task resumed', color: 'green' })
          break
        case 'delete':
          await taskService.delete(task.id)
          notifications.show({ title: 'Task deleted', color: 'red' })
          break
      }
      onRefresh()
    } catch (error) {
      notifications.show({
        title: 'Action failed',
        message: error.response?.data?.error || 'Something went wrong',
        color: 'red',
      })
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleString()
  }

  return (
    <Card shadow="sm" padding="md" radius="md" withBorder>
      <Group justify="space-between" mb="xs">
        <Group>
          <Text fw={600} size="lg" style={{ cursor: 'pointer' }} onClick={() => navigate(`/tasks/${task.id}`)}>
            {task.jobTitle}
          </Text>
          <Badge color={statusColors[task.status] || 'gray'} variant="light">
            {task.status}
          </Badge>
        </Group>
        <Group gap="xs">
          <ActionIcon variant="subtle" onClick={() => setExpanded(!expanded)}>
            {expanded ? <IconChevronUp size={18} /> : <IconChevronDown size={18} />}
          </ActionIcon>
          <Menu shadow="md" width={150}>
            <Menu.Target>
              <ActionIcon variant="subtle" loading={loading}>
                <IconDotsVertical size={18} />
              </ActionIcon>
            </Menu.Target>
            <Menu.Dropdown>
              {task.status === 'PAUSED' ? (
                <Menu.Item leftSection={<IconPlayerPlay size={16} />} onClick={() => handleAction('resume')}>
                  Resume
                </Menu.Item>
              ) : task.status === 'SCHEDULED' || task.status === 'COMPLETED' || task.status === 'FAILED' ? (
                <>
                  <Menu.Item leftSection={<IconRefresh size={16} />} onClick={() => handleAction('run')}>
                    Run Now
                  </Menu.Item>
                  <Menu.Item leftSection={<IconPlayerPause size={16} />} onClick={() => handleAction('pause')}>
                    Pause
                  </Menu.Item>
                </>
              ) : null}
              <Menu.Divider />
              <Menu.Item leftSection={<IconTrash size={16} />} color="red" onClick={() => handleAction('delete')}>
                Delete
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
      </Group>

      <Text size="sm" c="dimmed" mb="xs">
        {task.location}
      </Text>

      <Collapse in={expanded}>
        <Stack gap="xs" mt="md">
          <Group justify="space-between">
            <Text size="sm" c="dimmed">Number of Jobs:</Text>
            <Text size="sm">{task.numJobs}</Text>
          </Group>
          <Group justify="space-between">
            <Text size="sm" c="dimmed">Interval:</Text>
            <Text size="sm">{task.intervalHours ? `Every ${task.intervalHours} hours` : 'One-time'}</Text>
          </Group>
          <Group justify="space-between">
            <Text size="sm" c="dimmed">Last Run:</Text>
            <Text size="sm">{formatDate(task.lastRunAt)}</Text>
          </Group>
          <Group justify="space-between">
            <Text size="sm" c="dimmed">Next Run:</Text>
            <Text size="sm">{formatDate(task.nextRunAt)}</Text>
          </Group>
          {task.skills && task.skills.length > 0 && (
            <Group>
              <Text size="sm" c="dimmed">Skills:</Text>
              <Group gap={4}>
                {task.skills.map((skill, idx) => (
                  <Badge key={idx} size="sm" variant="outline">{skill}</Badge>
                ))}
              </Group>
            </Group>
          )}
          {task.lastError && (
            <Text size="sm" c="red">Error: {task.lastError}</Text>
          )}
        </Stack>
      </Collapse>
    </Card>
  )
}

export default function DashboardPage() {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpened, setModalOpened] = useState(false)

  const fetchTasks = async () => {
    try {
      const data = await taskService.getAll()
      setTasks(data)
    } catch (error) {
      notifications.show({
        title: 'Failed to load tasks',
        message: error.response?.data?.error || 'Something went wrong',
        color: 'red',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchTasks()
  }, [])

  if (loading) {
    return (
      <Center h={400}>
        <Loader size="lg" />
      </Center>
    )
  }

  return (
    <>
      <Group justify="space-between" mb="lg">
        <Title order={2}>Dashboard</Title>
        <Button leftSection={<IconPlus size={18} />} onClick={() => setModalOpened(true)}>
          Create Task
        </Button>
      </Group>

      {tasks.length === 0 ? (
        <Paper p="xl" withBorder>
          <Center>
            <Stack align="center" gap="md">
              <Text c="dimmed">No tasks yet</Text>
              <Button variant="light" onClick={() => setModalOpened(true)}>
                Create your first task
              </Button>
            </Stack>
          </Center>
        </Paper>
      ) : (
        <Stack gap="md">
          {tasks && tasks.map((task) => (
            <TaskCard key={task.id} task={task} onRefresh={fetchTasks} />
          ))}
        </Stack>
      )}

      <CreateTaskModal
        opened={modalOpened}
        onClose={() => setModalOpened(false)}
        onSuccess={() => {
          setModalOpened(false)
          fetchTasks()
        }}
      />
    </>
  )
}
