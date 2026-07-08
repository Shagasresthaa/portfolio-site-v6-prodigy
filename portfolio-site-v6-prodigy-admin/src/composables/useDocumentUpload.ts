import { authFetch } from '@/composables/useApi'

export async function uploadDocument(file: File, category: string): Promise<string> {
  const formData = new FormData()
  formData.append('file', file, file.name)

  const response = await authFetch(`/api/admin/uploads/document?category=${encodeURIComponent(category)}`, {
    method: 'POST',
    body: formData,
  })
  if (!response.ok) throw new Error('Failed to upload document.')

  const { url } = (await response.json()) as { url: string }
  return url
}
