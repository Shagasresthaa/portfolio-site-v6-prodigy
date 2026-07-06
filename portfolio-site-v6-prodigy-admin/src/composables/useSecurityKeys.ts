import { ref } from 'vue'
import { authFetch } from '@/composables/useApi'
import { registerSecurityKey as createRegistration } from '@/composables/useWebAuthn'

export interface SecurityKey {
  id: string
  label: string
  createdAt: string
  lastUsedAt: string | null
}

export function useSecurityKeys() {
  const keys = ref<SecurityKey[]>([])
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    try {
      const response = await authFetch('/api/admin/webauthn/credentials')
      if (!response.ok) throw new Error('Failed to load security keys.')
      keys.value = (await response.json()) as SecurityKey[]
    } finally {
      loading.value = false
    }
  }

  async function registerKey(label: string) {
    const trimmed = label.trim()
    if (!trimmed) throw new Error('Give the key a name.')

    const optionsResponse = await authFetch('/api/admin/webauthn/register/options', { method: 'POST' })
    if (!optionsResponse.ok) throw new Error('Failed to start registration.')
    const options = (await optionsResponse.json()) as PublicKeyCredentialCreationOptionsJSON

    const registrationJSON = await createRegistration(options)

    const verifyResponse = await authFetch(
      `/api/admin/webauthn/register/verify?label=${encodeURIComponent(trimmed)}`,
      { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: registrationJSON },
    )
    if (!verifyResponse.ok) throw new Error('Failed to register security key.')

    await refresh()
  }

  async function removeKey(id: string) {
    const response = await authFetch(`/api/admin/webauthn/credentials/${id}`, { method: 'DELETE' })
    if (!response.ok) throw new Error('Failed to remove security key.')
    keys.value = keys.value.filter((key) => key.id !== id)
  }

  return { keys, loading, refresh, registerKey, removeKey }
}
