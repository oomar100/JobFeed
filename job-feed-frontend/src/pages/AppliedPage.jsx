import { useState, useEffect } from 'react'
import {
  Title,
  Text,
  Group,
  Stack,
  Card,
  Badge,
  Loader,
  Center,
  Paper,
  Collapse,
  ActionIcon,
  Anchor,
  Divider,
  TextInput,
  Pagination,
} from '@mantine/core'
import {
  IconChevronDown,
  IconChevronUp,
  IconExternalLink,
  IconX,
  IconTrash,
  IconSearch,
} from '@tabler/icons-react'
import { notifications } from '@mantine/notifications'
import { jobService } from '../services/jobService'

function AppliedJobCard({ job, onRefresh }) {
  const [expanded, setExpanded] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleRemoveApplied = async () => {
    setLoading(true)
    try {
      await jobService.removeFromApplied(job.id)
      notifications.show({ title: 'Removed from applied', color: 'yellow' })
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
          <ActionIcon variant="subtle" color="yellow" onClick={handleRemoveApplied} loading={loading} title="Remove from applied">
            <IconX size={18} />
          </ActionIcon>
          <ActionIcon variant="subtle" color="red" onClick={handleDelete} loading={loading} title="Delete">
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
              <Text size="sm" style={{ whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto' }}>
                {job.description}
              </Text>
            </>
          )}
        </Stack>
      </Collapse>
    </Card>
  )
}

export default function AppliedPage() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [page, setPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const pageSize = 20

  const fetchJobs = async () => {
    setLoading(true)
    try {
      const params = {
        page: page - 1,
        size: pageSize,
      }
      const data = await jobService.getApplied(params)
      
      // Handle paginated response
      if (data.content) {
        setJobs(data.content)
        setTotalPages(data.totalPages || 1)
      } else {
        // Handle array response
        setJobs(Array.isArray(data) ? data : [])
        setTotalPages(1)
      }
    } catch (error) {
      notifications.show({
        title: 'Failed to load jobs',
        message: error.response?.data?.error || 'Something went wrong',
        color: 'red',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchJobs()
  }, [page])

  const filteredJobs = jobs.filter((job) => {
    if (!searchQuery) return true
    const query = searchQuery.toLowerCase()
    return (
      job.jobTitle?.toLowerCase().includes(query) ||
      job.companyName?.toLowerCase().includes(query) ||
      job.location?.toLowerCase().includes(query)
    )
  })

  if (loading && jobs.length === 0) {
    return (
      <Center h={400}>
        <Loader size="lg" />
      </Center>
    )
  }

  return (
    <>
      <Title order={2} mb="lg">Applied Jobs</Title>

      <Paper p="md" withBorder mb="lg">
        <TextInput
          placeholder="Search applied jobs..."
          leftSection={<IconSearch size={16} />}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </Paper>

      {filteredJobs.length === 0 ? (
        <Paper p="xl" withBorder>
          <Center>
            <Text c="dimmed">
              {searchQuery ? 'No jobs match your search' : 'No applied jobs yet'}
            </Text>
          </Center>
        </Paper>
      ) : (
        <>
          <Text size="sm" c="dimmed" mb="md">
            {filteredJobs.length} applied job{filteredJobs.length !== 1 ? 's' : ''}
          </Text>
          <Stack gap="sm">
            {filteredJobs.map((job) => (
              <AppliedJobCard key={job.id} job={job} onRefresh={fetchJobs} />
            ))}
          </Stack>
          {totalPages > 1 && (
            <Center mt="lg">
              <Pagination value={page} onChange={setPage} total={totalPages} />
            </Center>
          )}
        </>
      )}
    </>
  )
}
