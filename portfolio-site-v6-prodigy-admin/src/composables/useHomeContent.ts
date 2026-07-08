import { authFetch, getApiBaseUrl } from '@/composables/useApi'
import type { TimelineEvent } from '@/types/timeline'

export interface HomeContent {
  aboutHook: string
  aboutStory: string[]
  timeline: TimelineEvent[]
  resumeUrl: string | null
}

export async function fetchHomeContent(): Promise<HomeContent> {
  const response = await fetch(`${getApiBaseUrl()}/api/home`)
  if (!response.ok) throw new Error(`Failed to fetch home content: ${response.status}`)
  return (await response.json()) as HomeContent
}

export async function saveHomeContent(content: HomeContent): Promise<HomeContent> {
  const response = await authFetch('/api/admin/home', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(content),
  })
  if (!response.ok) throw new Error('Failed to save home content.')
  return (await response.json()) as HomeContent
}
