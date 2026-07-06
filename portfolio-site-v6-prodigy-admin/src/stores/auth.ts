import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'
import { decodeJwtExpiryMs } from '@/utils/jwt'

const STORAGE_KEY = 'admin-session'
const PASSWORD_OVERRIDE_KEY = 'admin-password-override'
const USERNAME_OVERRIDE_KEY = 'admin-username-override'

interface Session {
  username: string
  token: string
  expiresAt: number
}

/**
 * Used only by resetPassword()/verifyPassword() below, which are still mocked -
 * there's no real /api/admin/auth/* endpoint for those yet. login() itself no
 * longer uses this, see apiLogin().
 */
function getExpectedPassword(): string {
  const override = localStorage.getItem(PASSWORD_OVERRIDE_KEY)
  if (override !== null) return override
  return import.meta.env.PUBLIC_ADMIN_PASSWORD || 'admin'
}

async function apiLogin(username: string, password: string): Promise<Session> {
  const response = await fetch(`${getApiBaseUrl()}/api/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (response.status === 401) {
    throw new Error('Incorrect username or password.')
  }
  if (response.status === 501) {
    throw new Error('Second-factor login is not built yet - the bypass override must be enabled in the database.')
  }
  if (!response.ok) {
    throw new Error('Failed to sign in.')
  }

  const { token } = (await response.json()) as { token: string }
  return { username, token, expiresAt: decodeJwtExpiryMs(token) }
}

function readSession(): Session | null {
  // Astro prerenders this island's initial markup on the server (even with
  // client:load), where localStorage/window don't exist - fall back to a
  // logged-out state there; the client-side hydration pass re-runs this
  // with the real value.
  if (typeof localStorage === 'undefined') return null

  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null

  try {
    const session = JSON.parse(raw) as Session
    if (session.expiresAt < Date.now()) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return session
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(readSession())
  const isAuthenticated = computed(() => session.value !== null)
  const username = computed(() => session.value?.username ?? null)

  async function login(usernameInput: string, password: string) {
    const newSession = await apiLogin(usernameInput, password)
    session.value = newSession
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newSession))
  }

  function logout() {
    session.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  async function resetPassword(currentPassword: string, newPassword: string) {
    if (currentPassword !== getExpectedPassword()) {
      throw new Error('Current password is incorrect.')
    }
    localStorage.setItem(PASSWORD_OVERRIDE_KEY, newPassword)
  }

  async function changeUsername(currentPassword: string, newUsername: string) {
    if (currentPassword !== getExpectedPassword()) {
      throw new Error('Current password is incorrect.')
    }
    const trimmed = newUsername.trim()
    if (!trimmed) {
      throw new Error('Username cannot be empty.')
    }

    localStorage.setItem(USERNAME_OVERRIDE_KEY, trimmed)
    if (session.value) {
      session.value = { ...session.value, username: trimmed }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session.value))
    }
  }

  function verifyPassword(password: string): boolean {
    return password === getExpectedPassword()
  }

  return {
    session,
    isAuthenticated,
    username,
    login,
    logout,
    resetPassword,
    changeUsername,
    verifyPassword,
  }
})
