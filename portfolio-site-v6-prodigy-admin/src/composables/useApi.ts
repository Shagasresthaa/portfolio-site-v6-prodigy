import { useAuthStore } from '@/stores/auth'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'

export { getApiBaseUrl }

// Attaches the current session's bearer token. On 401 (expired/invalid token),
// logs out and sends the user back to the login page rather than surfacing a
// confusing error from whatever called this.
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
