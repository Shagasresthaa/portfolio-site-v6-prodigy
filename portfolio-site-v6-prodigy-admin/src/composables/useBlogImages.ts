import { authFetch } from '@/composables/useApi'
import type { BlogImage } from '@/types/blogImage'

export async function loadBlogImages(): Promise<BlogImage[]> {
  const response = await authFetch('/api/admin/blog/images')
  if (!response.ok) throw new Error(`Failed to fetch blog images: ${response.status}`)
  return (await response.json()) as BlogImage[]
}

export async function createBlogImage(url: string, altText: string | undefined): Promise<BlogImage> {
  const response = await authFetch('/api/admin/blog/images', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url, altText }),
  })
  if (!response.ok) throw new Error('Failed to save image.')
  return (await response.json()) as BlogImage
}

export async function deleteBlogImage(id: string): Promise<void> {
  const response = await authFetch(`/api/admin/blog/images/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('Failed to delete image.')
}
