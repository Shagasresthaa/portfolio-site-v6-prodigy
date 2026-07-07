import { authFetch, getApiBaseUrl } from '@/composables/useApi'
import type { Project } from '@/types/project'

export async function loadProjects(): Promise<Project[]> {
  const response = await fetch(`${getApiBaseUrl()}/api/projects`)
  if (!response.ok) throw new Error(`Failed to fetch projects: ${response.status}`)
  return (await response.json()) as Project[]
}

export async function createProject(project: Omit<Project, 'id'>): Promise<Project> {
  const response = await authFetch('/api/admin/projects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(project),
  })
  if (!response.ok) throw new Error('Failed to create project.')
  return (await response.json()) as Project
}

export async function updateProject(id: string, project: Omit<Project, 'id'>): Promise<Project> {
  const response = await authFetch(`/api/admin/projects/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(project),
  })
  if (!response.ok) throw new Error('Failed to update project.')
  return (await response.json()) as Project
}

export async function deleteProject(id: string): Promise<void> {
  const response = await authFetch(`/api/admin/projects/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('Failed to delete project.')
}
