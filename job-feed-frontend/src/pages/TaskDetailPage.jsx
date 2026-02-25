import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Title,
  Text,
  Button,
  Group,
  Stack,
  Card,
  Badge,
  Loader,
  Center,
  Paper,
  Divider,
  Collapse,
  ActionIcon,
  Anchor,
} from '@mantine/core'
import {
  IconArrowLeft,
  IconChevronDown,
  IconChevronUp,
  IconExternalLink,
  IconCheck,
  IconTrash,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { taskService } from '../services/taskService'
import { jobService } from '../services/jobService'

const statusColors = {
  SCHEDULED: 'blue',
  SCRAPING: 'yellow',
  RANKING: 'orange',
  COMPLETED: 'green',
  FAILED: 'red',
  PAUSED: 'gray',
}

function JobCard({ job, onRefresh }) {
  const [expanded, setExpanded] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleMarkApplied = async () => {
    setLoading(true)
    try {
      await jobService.markAsApplied(job.id)
      notifications.show({ title: 'Marked as applied', color: 'green' })
      onRefresh()
    } catch (error) {
      notifications.show({ title: 'Failed', color: 'red' })
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async () => {
    setLoading(true)
    try {
      await jobService.delete(job.id)
      notifications.show({ title: 'Job deleted', color: 'red' })
      onRefresh()
    } catch (error) {
      notifications.show({ title: 'Failed', color: 'red' })
    } finally {
      setLoading(false)
    }
  }

  const getScoreColor = (score) => {
    if (score >= 8) return 'green'
    if (score >= 5) return 'yellow'
    return 'red'
  }

  return (
    <Card shadow="xs" padding="sm" radius="md" withBorder>
      <Group justify="space-between">
        <Group>
          <Badge size="lg" color={getScoreColor(job.score)} variant="filled">
            {job.score}/10
          </Badge>
          <div>
            <Text fw={500}>{job.jobTitle}</Text>
            <Text size="sm" c="dimmed">{job.companyName} • {job.location}</Text>
          </div>
        </Group>
        <Group gap="xs">
          {job.bucket !== 'APPLIED' && (
            <ActionIcon variant="subtle" color="green" onClick={handleMarkApplied} loading={loading}>
              <IconCheck size={18} />
            </ActionIcon>
          )}
          {job.bucket === 'APPLIED' && (
            <Badge color="green" variant="light">Applied</Badge>
          )}
          <ActionIcon variant="subtle" color="red" onClick={handleDelete} loading={loading}>
            <IconTrash size={18} />
          </ActionIcon>
          <ActionIcon variant="subtle" onClick={() => setExpanded(!expanded)}>
            {expanded ? <IconChevronUp size={18} /> : <IconChevronDown size={18} />}
          </ActionIcon>
        </Group>
      </Group>

      <Collapse in={expanded}>
        <Stack gap="sm" mt="md">
          {job.salary && (
            <Group>
              <Text size="sm" c="dimmed">Salary:</Text>
              <Text size="sm">{job.salary}</Text>
            </Group>
          )}
          {job.jobUrl && (
            <Anchor href={job.jobUrl} target="_blank" size="sm">
              View on Indeed <IconExternalLink size={14} style={{ marginLeft: 4 }} />
            </Anchor>
          )}
          {job.description && (
            <>
              <Divider />
              <Text size="sm" style={{ whiteSpace: 'pre-wrap' }}>
                {job.description}
              </Text>
            </>
          )}
        </Stack>
      </Collapse>
    </Card>
  )
}

export default function TaskDetailPage() {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)

  const fetchData = async () => {
    try {
      const [taskData, jobsData] = await Promise.all([
        taskService.getById(taskId),
        jobService.getByTask(taskId),
      ])
      setTask(taskData)
      setJobs(jobsData)
    } catch (error) {
      notifications.show({
        title: 'Failed to load data',
        message: error.response?.data?.error || 'Something went wrong',
        color: 'red',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [taskId])

  if (loading) {
    return (
      <Center h={400}>
        <Loader size="lg" />
      </Center>
    )
  }

  if (!task) {
    return (
      <Center h={400}>
        <Text>Task not found</Text>
      </Center>
    )
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleString()
  }

  return (
    <>
      <Button
        variant="subtle"
        leftSection={<IconArrowLeft size={18} />}
        onClick={() => navigate('/')}
        mb="md"
      >
        Back to Dashboard
      </Button>

      <Paper p="md" withBorder mb="lg">
        <Group justify="space-between" mb="md">
          <div>
            <Title order={2}>{task.jobTitle}</Title>
            <Text c="dimmed">{task.location}</Text>
          </div>
          <Badge size="lg" color={statusColors[task.status] || 'gray'}>
            {task.status}
          </Badge>
        </Group>

        <Divider mb="md" />

        <Stack gap="xs">
          <Group>
            <Text size="sm" c="dimmed" w={150}>Number of Jobs:</Text>
            <Text size="sm">{task.numJobs}</Text>
          </Group>
          <Group>
            <Text size="sm" c="dimmed" w={150}>Interval:</Text>
            <Text size="sm">{task.intervalHours ? `Every ${task.intervalHours} hours` : 'One-time'}</Text>
          </Group>
          <Group>
            <Text size="sm" c="dimmed" w={150}>Last Run:</Text>
            <Text size="sm">{formatDate(task.lastRunAt)}</Text>
          </Group>
          <Group>
            <Text size="sm" c="dimmed" w={150}>Next Run:</Text>
            <Text size="sm">{formatDate(task.nextRunAt)}</Text>
          </Group>
          {task.skills && task.skills.length > 0 && (
            <Group>
              <Text size="sm" c="dimmed" w={150}>Skills:</Text>
              <Group gap={4}>
                {task.skills.map((skill, idx) => (
                  <Badge key={idx} size="sm" variant="outline">{skill}</Badge>
                ))}
              </Group>
            </Group>
          )}
        </Stack>
      </Paper>

      <Title order={3} mb="md">Jobs ({jobs.length})</Title>

      {jobs.length === 0 ? (
        <Paper p="xl" withBorder>
          <Center>
            <Text c="dimmed">No jobs scraped yet</Text>
          </Center>
        </Paper>
      ) : (
        <Stack gap="sm">
          {jobs.map((job) => (
            <JobCard key={job.id} job={job} onRefresh={fetchData} />
          ))}
        </Stack>
      )}
    </>
  )
}
