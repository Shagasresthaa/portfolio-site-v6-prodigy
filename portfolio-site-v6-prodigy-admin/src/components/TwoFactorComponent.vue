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
        <h1 class="font-salsa mb-2 text-3xl">Two-factor authentication</h1>
        <p class="text-ink-muted text-sm">Require an authenticator app code at login.</p>
      </div>

      <div
        class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-4 rounded-2xl border p-8 shadow-xl"
      >
        <p class="text-warning text-xs">
          This is a UI mock - codes aren't actually verified against the secret yet. Real TOTP
          enrollment needs the API to generate/store the secret and validate codes server-side.
        </p>

        <!-- Enabled -->
        <template v-if="isEnabled && !revealedBackupCodes">
          <p class="text-secondary flex items-center gap-2 text-sm">
            <ShieldCheckIcon class="size-5" aria-hidden="true" />
            Authenticator app is enabled.
          </p>
          <button
            type="button"
            class="border-ink-muted/30 hover:bg-surface rounded-lg border px-4 py-2 text-sm transition"
            @click="handleRegenerateBackupCodes"
          >
            Regenerate backup codes
          </button>

          <div class="border-ink-muted/20 mt-2 border-t pt-4">
            <label class="mb-2 block text-sm font-semibold" for="disable-password"
              >Current password</label
            >
            <div class="relative">
              <input
                id="disable-password"
                v-model="disablePassword"
                :type="showDisablePassword ? 'text' : 'password'"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base focus:outline-none"
                @input="error = null"
              />
              <PasswordVisibilityToggle v-model="showDisablePassword" />
            </div>
            <button
              type="button"
              class="text-danger mt-3 text-sm hover:opacity-80"
              @click="handleDisable"
            >
              Disable two-factor authentication
            </button>
          </div>
        </template>

        <!-- Just generated/regenerated backup codes -->
        <template v-else-if="revealedBackupCodes">
          <p class="text-sm">Save these backup codes somewhere safe - each can be used once.</p>
          <ul class="bg-surface grid grid-cols-2 gap-2 rounded-lg p-4 font-mono text-sm">
            <li v-for="code in revealedBackupCodes" :key="code">{{ code }}</li>
          </ul>
          <button
            type="button"
            class="bg-primary text-primary-contrast rounded-lg px-4 py-2 text-sm transition hover:opacity-90"
            @click="revealedBackupCodes = null"
          >
            Done
          </button>
        </template>

        <!-- Mid-enrollment -->
        <template v-else-if="pendingSecret">
          <p class="text-sm">
            Add this key to your authenticator app (Google Authenticator, Authy, etc.):
          </p>
          <p class="bg-surface rounded-lg p-3 font-mono text-sm break-all">
            {{ pendingSecret }}
          </p>
          <div>
            <label class="mb-2 block text-sm font-semibold" for="totp-code">6-digit code</label>
            <input
              id="totp-code"
              v-model="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="000000"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base focus:outline-none"
              @input="error = null"
            />
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              class="bg-primary text-primary-contrast rounded-lg px-4 py-2 text-sm transition hover:opacity-90"
              @click="handleConfirm"
            >
              Confirm
            </button>
            <button
              type="button"
              class="border-ink-muted/30 hover:bg-surface rounded-lg border px-4 py-2 text-sm transition"
              @click="cancelEnrollment"
            >
              Cancel
            </button>
          </div>
        </template>

        <!-- Not enabled -->
        <template v-else>
          <p class="text-ink-muted flex items-center gap-2 text-sm">
            <ShieldExclamationIcon class="size-5" aria-hidden="true" />
            Two-factor authentication is not enabled.
          </p>
          <button
            type="button"
            class="bg-primary text-primary-contrast rounded-lg px-4 py-2 text-sm transition hover:opacity-90"
            @click="pendingSecret = startEnrollment()"
          >
            Enable authenticator app
          </button>
        </template>

        <p v-if="error" class="text-danger text-sm">{{ error }}</p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  ArrowLeftIcon,
  ShieldCheckIcon,
  ShieldExclamationIcon,
} from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import PasswordVisibilityToggle from '@/components/PasswordVisibilityToggle.vue'
import { useTwoFactor } from '@/composables/useTwoFactor'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const {
  isEnabled,
  pendingSecret,
  startEnrollment,
  confirmEnrollment,
  cancelEnrollment,
  disable,
  regenerateBackupCodes,
} = useTwoFactor()

const code = ref('')
const disablePassword = ref('')
const showDisablePassword = ref(false)
const revealedBackupCodes = ref<string[] | null>(null)
const error = ref<string | null>(null)

function handleConfirm() {
  try {
    revealedBackupCodes.value = confirmEnrollment(code.value)
    code.value = ''
    error.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to confirm code.'
  }
}

function handleRegenerateBackupCodes() {
  try {
    revealedBackupCodes.value = regenerateBackupCodes()
    error.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to regenerate backup codes.'
  }
}

function handleDisable() {
  if (!authStore.verifyPassword(disablePassword.value)) {
    error.value = 'Current password is incorrect.'
    return
  }
  disable()
  disablePassword.value = ''
  error.value = null
}
</script>
