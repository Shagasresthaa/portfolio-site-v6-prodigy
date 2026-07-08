import { authFetch, getApiBaseUrl } from '@/composables/useApi'
import type { Certificate } from '@/types/certificate'

export async function loadCertificates(): Promise<Certificate[]> {
  const response = await fetch(`${getApiBaseUrl()}/api/certificates`)
  if (!response.ok) throw new Error(`Failed to fetch certificates: ${response.status}`)
  return (await response.json()) as Certificate[]
}

export async function createCertificate(imageUrl: string, title: string | undefined): Promise<Certificate> {
  const response = await authFetch('/api/admin/certificates', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ imageUrl, title }),
  })
  if (!response.ok) throw new Error('Failed to save certificate.')
  return (await response.json()) as Certificate
}

export async function deleteCertificate(id: string): Promise<void> {
  const response = await authFetch(`/api/admin/certificates/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('Failed to delete certificate.')
}
