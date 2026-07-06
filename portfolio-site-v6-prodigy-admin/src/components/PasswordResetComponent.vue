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

      <div class="font-salsa mb-6 text-center">
        <h1 class="mb-2 text-3xl">Reset password</h1>
        <p class="text-ink-muted text-sm">Change the password used to sign in.</p>
      </div>

      <form
        class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
        @submit.prevent="handleSubmit"
      >
        <div>
          <label class="mb-2 block text-sm font-semibold" for="current-password"
            >Current password</label
          >
          <div class="relative">
            <input
              id="current-password"
              v-model="currentPassword"
              :type="showCurrent ? 'text' : 'password'"
              required
              autocomplete="current-password"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base focus:outline-none"
              @input="clearMessages"
            />
            <PasswordVisibilityToggle v-model="showCurrent" />
          </div>
        </div>

        <div>
          <label class="mb-2 block text-sm font-semibold" for="new-password">New password</label>
          <div class="relative">
            <input
              id="new-password"
              v-model="newPassword"
              :type="showNew ? 'text' : 'password'"
              required
              minlength="8"
              autocomplete="new-password"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base focus:outline-none"
              @input="clearMessages"
            />
            <PasswordVisibilityToggle v-model="showNew" />
          </div>
          <p class="text-ink-muted mt-1 text-xs">At least 8 characters.</p>
        </div>

        <div>
          <label class="mb-2 block text-sm font-semibold" for="confirm-password"
            >Confirm new password</label
          >
          <div class="relative">
            <input
              id="confirm-password"
              v-model="confirmPassword"
              :type="showConfirm ? 'text' : 'password'"
              required
              autocomplete="new-password"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base focus:outline-none"
              @input="clearMessages"
            />
            <PasswordVisibilityToggle v-model="showConfirm" />
          </div>
        </div>

        <p v-if="error" class="text-danger -mt-2 text-sm">{{ error }}</p>
        <p v-if="success" class="text-secondary -mt-2 text-sm">Password updated.</p>

        <button
          type="submit"
          :disabled="submitting"
          class="bg-primary text-primary-contrast font-salsa rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
        >
          {{ submitting ? 'Updating…' : 'Update password' }}
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

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

function clearMessages() {
  error.value = null
  success.value = false
}

async function handleSubmit() {
  if (newPassword.value !== confirmPassword.value) {
    error.value = 'New password and confirmation do not match.'
    return
  }

  submitting.value = true
  error.value = null
  try {
    await authStore.resetPassword(currentPassword.value, newPassword.value)
    success.value = true
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to update password.'
  } finally {
    submitting.value = false
  }
}
</script>
