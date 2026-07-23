import { useAuthStore } from '@/stores/auth'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'

export { getApiBaseUrl }

// Attaches the bearer token; force-logs-out and redirects to `/` on 401.
export async function authFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const authStore = useAuthStore()
  const headers = new Headers(init.headers)
  if (authStore.session) {
    headers.set('Authorization', `Bearer ${authStore.session.token}`)
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, { ...init, headers })

  if (response.status === 401) {
    authStore.logout()
    window.location.href = '/'
  }

  return response
}
