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
        <h1 class="font-salsa mb-2 text-3xl">Change username</h1>
        <p class="text-ink-muted text-sm">
          Currently <span class="text-ink font-semibold">{{ authStore.username }}</span>
        </p>
      </div>

      <form
        class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
        @submit.prevent="handleSubmit"
      >
        <div>
          <label class="mb-2 block text-sm font-semibold" for="new-username">New username</label>
          <input
            id="new-username"
            v-model="newUsername"
            type="text"
            required
            autocomplete="username"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base focus:outline-none"
            @input="clearMessages"
          />
        </div>

        <div>
          <label class="mb-2 block text-sm font-semibold" for="current-password"
            >Current password</label
          >
          <div class="relative">
            <input
              id="current-password"
              v-model="currentPassword"
              :type="showPassword ? 'text' : 'password'"
              required
              autocomplete="current-password"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base focus:outline-none"
              @input="clearMessages"
            />
            <PasswordVisibilityToggle v-model="showPassword" />
          </div>
          <p class="text-ink-muted mt-1 text-xs">Confirm it's you before changing your username.</p>
        </div>

        <p v-if="error" class="text-danger -mt-2 text-sm">{{ error }}</p>
        <p v-if="success" class="text-secondary -mt-2 text-sm">Username updated.</p>

        <button
          type="submit"
          :disabled="submitting"
          class="bg-primary text-primary-contrast font-salsa rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
        >
          {{ submitting ? 'Updating…' : 'Update username' }}
        </button>
      </form>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeftIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import PasswordVisibilityToggle from '@/components/PasswordVisibilityToggle.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const newUsername = ref('')
const currentPassword = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

function clearMessages() {
  error.value = null
  success.value = false
}

async function handleSubmit() {
  submitting.value = true
  error.value = null
  try {
    await authStore.changeUsername(currentPassword.value, newUsername.value)
    success.value = true
    newUsername.value = ''
    currentPassword.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to update username.'
  } finally {
    submitting.value = false
  }
}
</script>
