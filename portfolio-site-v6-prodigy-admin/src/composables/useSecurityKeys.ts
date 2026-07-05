import { ref } from 'vue'

const STORAGE_KEY = 'admin-security-keys'

export interface SecurityKey {
  id: string
  name: string
  addedAt: number
}

function readKeys(): SecurityKey[] {
  if (typeof localStorage === 'undefined') return []
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return []
  try {
    return JSON.parse(raw) as SecurityKey[]
  } catch {
    return []
  }
}

function writeKeys(keys: SecurityKey[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(keys))
}

/**
 * Local-only stand-in for WebAuthn security key registration
 * (`navigator.credentials.create()`/`.get()`) - there's no relying-party
 * backend yet to verify/store a real credential against, so this just
 * persists a named placeholder entry. Swap for a real WebAuthn ceremony
 * once the API exposes registration/assertion endpoints for it.
 */
export function useSecurityKeys() {
  const keys = ref<SecurityKey[]>(readKeys())

  function addKey(name: string) {
    const trimmed = name.trim()
    if (!trimmed) throw new Error('Give the key a name.')
    const key: SecurityKey = { id: crypto.randomUUID(), name: trimmed, addedAt: Date.now() }
    keys.value = [...keys.value, key]
    writeKeys(keys.value)
  }

  function removeKey(id: string) {
    keys.value = keys.value.filter((key) => key.id !== id)
    writeKeys(keys.value)
  }

  return { keys, addKey, removeKey }
}
