import { authFetch, getApiBaseUrl } from '@/composables/useApi'
import type { HighlightItem } from '@/types/highlight'

export async function loadHighlights(): Promise<HighlightItem[]> {
  const response = await fetch(`${getApiBaseUrl()}/api/highlights`)
  if (!response.ok) throw new Error(`Failed to fetch highlights: ${response.status}`)
  return (await response.json()) as HighlightItem[]
}

export async function createHighlight(item: Omit<HighlightItem, 'id' | 'createdAt'>): Promise<HighlightItem> {
  const response = await authFetch('/api/admin/highlights', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(item),
  })
  if (!response.ok) throw new Error('Failed to create highlight.')
  return (await response.json()) as HighlightItem
}

export async function updateHighlight(
  id: string,
  item: Omit<HighlightItem, 'id' | 'createdAt'>,
): Promise<HighlightItem> {
  const response = await authFetch(`/api/admin/highlights/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(item),
  })
  if (!response.ok) throw new Error('Failed to update highlight.')
  return (await response.json()) as HighlightItem
}

export async function deleteHighlight(id: string): Promise<void> {
  const response = await authFetch(`/api/admin/highlights/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('Failed to delete highlight.')
}
