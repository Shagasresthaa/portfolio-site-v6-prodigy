import { authFetch } from '@/composables/useApi'

export async function uploadImage(blob: Blob, category: string): Promise<string> {
  const formData = new FormData()
  formData.append('file', blob, `image.${blob.type.split('/')[1] ?? 'webp'}`)

  const response = await authFetch(`/api/admin/uploads/image?category=${encodeURIComponent(category)}`, {
    method: 'POST',
    body: formData,
  })
  if (!response.ok) throw new Error('Failed to upload image.')

  const { url } = (await response.json()) as { url: string }
  return url
}
