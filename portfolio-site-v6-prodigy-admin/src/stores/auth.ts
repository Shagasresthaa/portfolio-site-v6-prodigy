import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const STORAGE_KEY = 'admin-session'
const PASSWORD_OVERRIDE_KEY = 'admin-password-override'
const USERNAME_OVERRIDE_KEY = 'admin-username-override'
const SESSION_TTL_MS = 24 * 60 * 60 * 1000 // 24h

interface Session {
  username: string
  token: string
  expiresAt: number
}

/**
 * The "current" username/password are normally PUBLIC_ADMIN_USERNAME/
 * PUBLIC_ADMIN_PASSWORD, but changeUsername()/resetPassword() below can
 * override them client-side (there's no real user table to persist to yet)
 * - the override, if present, wins.
 */
function getExpectedUsername(): string {
  const override = localStorage.getItem(USERNAME_OVERRIDE_KEY)
  if (override !== null) return override
  return import.meta.env.PUBLIC_ADMIN_USERNAME || 'admin'
}

function getExpectedPassword(): string {
  const override = localStorage.getItem(PASSWORD_OVERRIDE_KEY)
  if (override !== null) return override
  return import.meta.env.PUBLIC_ADMIN_PASSWORD || 'admin'
}

/**
 * Stand-in for the not-yet-built `/api/admin/auth/login` endpoint (see
 * CLAUDE.md: BlogController/ContactFormController/etc. are scaffolded but
 * empty). Validates against a single dev credential rather than a real user
 * table - falls back to admin/admin if PUBLIC_ADMIN_* env vars are unset.
 * `login()` is async and returns a fake bearer token so the calling code
 * (LoginForm.vue) already has the shape it'll need once this is swapped for
 * a real `fetch('/api/admin/auth/login', ...)` call.
 */
async function mockLogin(username: string, password: string): Promise<Session> {
  if (username !== getExpectedUsername() || password !== getExpectedPassword()) {
    throw new Error('Incorrect username or password.')
  }

  return {
    username,
    token: `mock-token.${btoa(`${username}:${Date.now()}`)}`,
    expiresAt: Date.now() + SESSION_TTL_MS,
  }
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
    const newSession = await mockLogin(usernameInput, password)
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
