import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  AppShell,
  NavLink,
  Group,
  Text,
  ActionIcon,
  Divider,
  Box,
  Burger,
} from '@mantine/core'
import {
  IconDashboard,
  IconBriefcase,
  IconCheck,
  IconLogout,
  IconSun,
  IconMoon,
} from '@tabler/icons-react'
import { useAuth } from '../../context/AuthContext'
import { useThemeContext } from '../../context/ThemeContext'

const navItems = [
  { label: 'Dashboard', icon: IconDashboard, path: '/' },
  { label: 'All Jobs', icon: IconBriefcase, path: '/jobs' },
  { label: 'Applied', icon: IconCheck, path: '/applied' },
]

export default function Layout() {
  const [opened, setOpened] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()
  const { colorScheme, toggleColorScheme } = useThemeContext()

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 250, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group>
            <Burger opened={opened} onClick={() => setOpened(!opened)} hiddenFrom="sm" size="sm" />
            <Text size="lg" fw={700}>Job Scraper</Text>
          </Group>
          <Group>
            <ActionIcon
              variant="subtle"
              onClick={toggleColorScheme}
              size="lg"
              aria-label="Toggle color scheme"
            >
              {colorScheme === 'dark' ? <IconSun size={20} /> : <IconMoon size={20} />}
            </ActionIcon>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md">
        <AppShell.Section grow>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              label={item.label}
              leftSection={<item.icon size={20} />}
              active={location.pathname === item.path}
              onClick={() => {
                navigate(item.path)
                setOpened(false)
              }}
              mb={4}
            />
          ))}
        </AppShell.Section>

        <AppShell.Section>
          <Divider my="sm" />
          <Box px="xs" py="sm">
            <Text size="sm" c="dimmed" mb="xs">
              {user?.firstName} {user?.lastName}
            </Text>
            <Text size="xs" c="dimmed" mb="sm">
              {user?.email}
            </Text>
          </Box>
          <NavLink
            label="Logout"
            leftSection={<IconLogout size={20} />}
            onClick={logout}
            c="red"
          />
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  )
}
