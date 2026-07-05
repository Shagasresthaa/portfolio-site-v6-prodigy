<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-md px-4 py-16">
      <a
        href="/account"
        class="text-ink-muted hover:text-ink font-salsa mb-6 flex items-center gap-1 text-sm"
      >
        <ArrowLeftIcon class="size-4" aria-hidden="true" />
        Back to account
      </a>

      <div class="mb-6 text-center">
        <h1 class="font-salsa mb-2 text-3xl">Security keys</h1>
        <p class="text-ink-muted text-sm">
          Hardware keys (YubiKey, etc.) for passwordless sign-in via WebAuthn.
        </p>
      </div>

      <div
        class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-4 rounded-2xl border p-8 shadow-xl"
      >
        <p class="text-warning text-xs">
          This is a UI mock - no real WebAuthn registration happens yet. Entries here are just
          named placeholders until the API has a relying-party endpoint to register against.
        </p>

        <ul v-if="keys.length > 0" class="flex flex-col gap-2">
          <li
            v-for="key in keys"
            :key="key.id"
            class="border-ink-muted/20 bg-surface flex items-center justify-between rounded-lg border p-3"
          >
            <div class="flex items-center gap-2">
              <FingerPrintIcon class="text-primary size-5" aria-hidden="true" />
              <div>
                <p>{{ key.name }}</p>
                <p class="text-ink-muted text-xs">Added {{ formatDate(key.addedAt) }}</p>
              </div>
            </div>
            <button
              type="button"
              class="text-danger text-sm hover:opacity-80"
              @click="removeKey(key.id)"
            >
              Remove
            </button>
          </li>
        </ul>
        <p v-else class="text-ink-muted text-sm">No security keys registered yet.</p>

        <form class="flex gap-2" @submit.prevent="handleAdd">
          <input
            v-model="newKeyName"
            type="text"
            placeholder="Key name (e.g. YubiKey 5C)"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base focus:outline-none"
            @input="error = null"
          />
          <button
            type="submit"
            class="bg-primary text-primary-contrast rounded-lg px-4 py-2 whitespace-nowrap transition hover:opacity-90"
          >
            Register key
          </button>
        </form>
        <p v-if="error" class="text-danger -mt-2 text-sm">{{ error }}</p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeftIcon, FingerPrintIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { useSecurityKeys } from '@/composables/useSecurityKeys'

const { keys, addKey, removeKey } = useSecurityKeys()

const newKeyName = ref('')
const error = ref<string | null>(null)

function formatDate(timestamp: number) {
  return new Date(timestamp).toLocaleDateString()
}

function handleAdd() {
  try {
    addKey(newKeyName.value)
    newKeyName.value = ''
    error.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to register key.'
  }
}
</script>
