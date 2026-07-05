import { ref } from 'vue'

const STORAGE_KEY = 'admin-2fa'

interface TwoFactorState {
  secret: string
  backupCodes: string[]
}

function readState(): TwoFactorState | null {
  if (typeof localStorage === 'undefined') return null
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as TwoFactorState
  } catch {
    return null
  }
}

function writeState(state: TwoFactorState | null) {
  if (state === null) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

function randomBase32Secret(length = 16): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  return Array.from({ length }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join(
    '',
  )
}

function generateBackupCodes(count = 8): string[] {
  return Array.from({ length: count }, () => {
    const a = Math.random().toString(36).slice(2, 6).toUpperCase()
    const b = Math.random().toString(36).slice(2, 6).toUpperCase()
    return `${a}-${b}`
  })
}

/**
 * Local-only stand-in for TOTP-based 2FA. There's no backend yet to
 * generate/store a real authenticator secret or verify submitted codes
 * server-side, so "confirming" here only checks the code *looks* like a
 * 6-digit TOTP code - it isn't actually validated against the secret.
 * Swap for a real enrollment/verification API once one exists.
 */
export function useTwoFactor() {
  const state = ref<TwoFactorState | null>(readState())
  const isEnabled = ref(state.value !== null)
  const pendingSecret = ref<string | null>(null)

  function startEnrollment(): string {
    pendingSecret.value = randomBase32Secret()
    return pendingSecret.value
  }

  function confirmEnrollment(code: string): string[] {
    if (!/^\d{6}$/.test(code.trim())) {
      throw new Error('Enter the 6-digit code from your authenticator app.')
    }
    if (!pendingSecret.value) {
      throw new Error('Start enrollment first.')
    }

    const backupCodes = generateBackupCodes()
    state.value = { secret: pendingSecret.value, backupCodes }
    isEnabled.value = true
    writeState(state.value)
    pendingSecret.value = null
    return backupCodes
  }

  function cancelEnrollment() {
    pendingSecret.value = null
  }

  function disable() {
    state.value = null
    isEnabled.value = false
    writeState(null)
  }

  function regenerateBackupCodes(): string[] {
    if (!state.value) throw new Error('Two-factor authentication is not enabled.')
    state.value = { ...state.value, backupCodes: generateBackupCodes() }
    writeState(state.value)
    return state.value.backupCodes
  }

  return {
    state,
    isEnabled,
    pendingSecret,
    startEnrollment,
    confirmEnrollment,
    cancelEnrollment,
    disable,
    regenerateBackupCodes,
  }
}
