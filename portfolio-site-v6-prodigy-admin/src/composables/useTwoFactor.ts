import { ref } from 'vue'
import { authFetch } from '@/composables/useApi'

interface EnrollmentOptions {
  secret: string
  otpauthUri: string
  qrCodeImagePng: string
}

export function useTwoFactor() {
  const isEnabled = ref(false)
  const remainingBackupCodes = ref(0)
  const loading = ref(false)
  const pendingSecret = ref<string | null>(null)
  const pendingQrCodeImagePng = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    try {
      const response = await authFetch('/api/admin/totp/status')
      if (!response.ok) throw new Error('Failed to load two-factor status.')
      const status = (await response.json()) as { enabled: boolean; remainingBackupCodes: number }
      isEnabled.value = status.enabled
      remainingBackupCodes.value = status.remainingBackupCodes
    } finally {
      loading.value = false
    }
  }

  async function startEnrollment() {
    const response = await authFetch('/api/admin/totp/enroll/begin', { method: 'POST' })
    if (!response.ok) throw new Error('Failed to start enrollment.')
    const options = (await response.json()) as EnrollmentOptions
    pendingSecret.value = options.secret
    pendingQrCodeImagePng.value = options.qrCodeImagePng
  }

  async function confirmEnrollment(code: string): Promise<string[]> {
    if (!/^\d{6}$/.test(code.trim())) {
      throw new Error('Enter the 6-digit code from your authenticator app.')
    }
    if (!pendingSecret.value) {
      throw new Error('Start enrollment first.')
    }

    const response = await authFetch(
      `/api/admin/totp/enroll/confirm?code=${encodeURIComponent(code.trim())}`,
      { method: 'POST' },
    )
    if (response.status === 400) {
      throw new Error('Invalid code - check the time on your device and try again.')
    }
    if (!response.ok) {
      throw new Error('Failed to confirm code.')
    }

    const backupCodes = (await response.json()) as string[]
    pendingSecret.value = null
    pendingQrCodeImagePng.value = null
    await refresh()
    return backupCodes
  }

  function cancelEnrollment() {
    pendingSecret.value = null
    pendingQrCodeImagePng.value = null
  }

  async function disable() {
    const response = await authFetch('/api/admin/totp', { method: 'DELETE' })
    if (!response.ok) throw new Error('Failed to disable two-factor authentication.')
    isEnabled.value = false
    remainingBackupCodes.value = 0
  }

  async function regenerateBackupCodes(): Promise<string[]> {
    const response = await authFetch('/api/admin/totp/backup-codes/regenerate', { method: 'POST' })
    if (!response.ok) throw new Error('Failed to regenerate backup codes.')
    const backupCodes = (await response.json()) as string[]
    await refresh()
    return backupCodes
  }

  return {
    isEnabled,
    remainingBackupCodes,
    loading,
    pendingSecret,
    pendingQrCodeImagePng,
    refresh,
    startEnrollment,
    confirmEnrollment,
    cancelEnrollment,
    disable,
    regenerateBackupCodes,
  }
}
