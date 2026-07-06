import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'
import { decodeJwtExpiryMs } from '@/utils/jwt'
import { assertSecurityKey } from '@/composables/useWebAuthn'

const STORAGE_KEY = 'admin-session'

interface Session {
  username: string
  token: string
  expiresAt: number
}

interface LoginResponseBody {
  status: 'OK' | 'WEBAUTHN_REQUIRED' | 'TOTP_REQUIRED' | 'SECOND_FACTOR_CHOICE_REQUIRED'
  token: string | null
  webAuthnOptions: string | null
  availableMethods: string[] | null
}

// What the login form needs to do next, once the password has checked out.
export type LoginOutcome =
  | { status: 'DONE' }
  | { status: 'TOTP_REQUIRED'; username: string }
  | { status: 'CHOICE_REQUIRED'; username: string; webAuthnOptions: string }

function sessionFromToken(username: string, token: string): Session {
  return { username, token, expiresAt: decodeJwtExpiryMs(token) }
}

// Authenticated request that does NOT log the user out on 401 - unlike useApi.ts's authFetch,
// a 401 from these account-management endpoints usually means "current password was wrong",
// not "your session is invalid", so each caller below interprets it itself.
async function authedFetch(session: Session, path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${session.token}`)
  return fetch(`${getApiBaseUrl()}${path}`, { ...init, headers })
}

async function completeWebAuthnLogin(username: string, webAuthnOptionsJSON: string): Promise<Session> {
  // webAuthnOptionsJSON is itself a JSON string (the API's `String` return type from the
  // webauthn4j ceremony, nested as one field of the login response's own JSON).
  const options = JSON.parse(webAuthnOptionsJSON) as PublicKeyCredentialRequestOptionsJSON
  const assertionJSON = await assertSecurityKey(options)

  const response = await fetch(
    `${getApiBaseUrl()}/api/admin/auth/webauthn/verify?username=${encodeURIComponent(username)}`,
    { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: assertionJSON },
  )
  if (response.status === 401) {
    throw new Error('Security key verification failed.')
  }
  if (!response.ok) {
    throw new Error('Failed to sign in.')
  }

  const { token } = (await response.json()) as { token: string }
  return sessionFromToken(username, token)
}

async function completeTotpLogin(username: string, code: string): Promise<Session> {
  const response = await fetch(`${getApiBaseUrl()}/api/admin/auth/totp/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, code }),
  })
  if (response.status === 401) {
    throw new Error('Invalid code.')
  }
  if (!response.ok) {
    throw new Error('Failed to sign in.')
  }

  const { token } = (await response.json()) as { token: string }
  return sessionFromToken(username, token)
}

async function passwordLogin(username: string, password: string): Promise<LoginResponseBody> {
  const response = await fetch(`${getApiBaseUrl()}/api/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (response.status === 401) {
    throw new Error('Incorrect username or password.')
  }
  if (response.status === 501) {
    throw new Error('No second factor registered and the bypass override is disabled - enable it in the database.')
  }
  if (!response.ok) {
    throw new Error('Failed to sign in.')
  }

  return (await response.json()) as LoginResponseBody
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

  function setSession(newSession: Session) {
    session.value = newSession
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newSession))
  }

  // Only asks the caller to do something further when a choice or a code is needed.
  // A lone WebAuthn requirement resolves itself here, same as before this method existed.
  async function login(usernameInput: string, password: string): Promise<LoginOutcome> {
    const body = await passwordLogin(usernameInput, password)

    if (body.status === 'OK') {
      setSession(sessionFromToken(usernameInput, body.token!))
      return { status: 'DONE' }
    }
    if (body.status === 'WEBAUTHN_REQUIRED') {
      setSession(await completeWebAuthnLogin(usernameInput, body.webAuthnOptions!))
      return { status: 'DONE' }
    }
    if (body.status === 'TOTP_REQUIRED') {
      return { status: 'TOTP_REQUIRED', username: usernameInput }
    }
    return { status: 'CHOICE_REQUIRED', username: usernameInput, webAuthnOptions: body.webAuthnOptions! }
  }

  async function loginWithSecurityKey(username: string, webAuthnOptions: string) {
    setSession(await completeWebAuthnLogin(username, webAuthnOptions))
  }

  async function loginWithTotp(username: string, code: string) {
    setSession(await completeTotpLogin(username, code))
  }

  function logout() {
    session.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  // Confirms the stored token is still genuinely valid against the server (and syncs the
  // canonical username) rather than trusting locally-cached state - see AuthGate.vue. Logs
  // out and returns false on any failure, so callers just need to redirect in that case.
  async function verifySession(): Promise<boolean> {
    if (!session.value) return false
    try {
      const response = await authedFetch(session.value, '/api/admin/account/me')
      if (!response.ok) {
        logout()
        return false
      }
      const { username: canonicalUsername } = (await response.json()) as { username: string }
      if (canonicalUsername !== session.value.username) {
        session.value = { ...session.value, username: canonicalUsername }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session.value))
      }
      return true
    } catch {
      logout()
      return false
    }
  }

  async function resetPassword(currentPassword: string, newPassword: string) {
    if (!session.value) throw new Error('Not signed in.')
    const response = await authedFetch(session.value, '/api/admin/account/password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newPassword }),
    })
    if (response.status === 401) {
      throw new Error('Current password is incorrect.')
    }
    if (response.status === 400) {
      throw new Error('New password must be at least 8 characters.')
    }
    if (!response.ok) {
      throw new Error('Failed to update password.')
    }
    const { token } = (await response.json()) as { token: string }
    setSession(sessionFromToken(session.value.username, token))
  }

  async function changeUsername(currentPassword: string, newUsername: string) {
    if (!session.value) throw new Error('Not signed in.')
    const response = await authedFetch(session.value, '/api/admin/account/username', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newUsername }),
    })
    if (response.status === 401) {
      throw new Error('Current password is incorrect.')
    }
    if (response.status === 409) {
      throw new Error('That username is already taken.')
    }
    if (response.status === 400) {
      throw new Error('Username cannot be empty.')
    }
    if (!response.ok) {
      throw new Error('Failed to update username.')
    }
    const { token } = (await response.json()) as { token: string }
    setSession(sessionFromToken(newUsername.trim(), token))
  }

  async function verifyPassword(password: string): Promise<boolean> {
    if (!session.value) return false
    const response = await authedFetch(session.value, '/api/admin/account/verify-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password }),
    })
    return response.ok
  }

  return {
    session,
    isAuthenticated,
    username,
    login,
    loginWithSecurityKey,
    loginWithTotp,
    logout,
    verifySession,
    resetPassword,
    changeUsername,
    verifyPassword,
  }
})
