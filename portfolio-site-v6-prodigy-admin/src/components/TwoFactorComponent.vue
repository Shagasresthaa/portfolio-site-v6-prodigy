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
        <h1 class="mb-2 text-3xl">Two-factor authentication</h1>
        <p class="text-ink-muted text-sm">Require an authenticator app code at login.</p>
      </div>

      <div
        class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-4 rounded-2xl border p-8 shadow-xl"
      >
        <p v-if="loading" class="text-ink-muted text-sm">Loading…</p>

        <!-- Enabled -->
        <template v-else-if="isEnabled && !revealedBackupCodes">
          <p class="text-secondary flex items-center gap-2 text-sm">
            <ShieldCheckIcon class="size-5" aria-hidden="true" />
            Authenticator app is enabled.
          </p>
          <p class="text-ink-muted text-xs">{{ remainingBackupCodes }} backup codes remaining.</p>
          <button
            type="button"
            :disabled="busy"
            class="border-ink-muted/30 hover:bg-surface rounded-lg border px-4 py-2 text-sm transition disabled:opacity-60"
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
              :disabled="busy"
              class="text-danger mt-3 text-sm hover:opacity-80 disabled:opacity-60"
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
            Scan this with your authenticator app (Google Authenticator, Authy, etc.):
          </p>
          <img
            v-if="pendingQrCodeImagePng"
            :src="`data:image/png;base64,${pendingQrCodeImagePng}`"
            alt="Two-factor authentication QR code"
            class="mx-auto rounded-lg bg-white p-2"
            width="200"
            height="200"
          />
          <p class="text-ink-muted text-xs">Or enter this key manually:</p>
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
              :disabled="busy"
              class="bg-primary text-primary-contrast rounded-lg px-4 py-2 text-sm transition hover:opacity-90 disabled:opacity-60"
              @click="handleConfirm"
            >
              {{ busy ? 'Confirming…' : 'Confirm' }}
            </button>
            <button
              type="button"
              :disabled="busy"
              class="border-ink-muted/30 hover:bg-surface rounded-lg border px-4 py-2 text-sm transition disabled:opacity-60"
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
            :disabled="busy"
            class="bg-primary text-primary-contrast rounded-lg px-4 py-2 text-sm transition hover:opacity-90 disabled:opacity-60"
            @click="handleStartEnrollment"
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
import { onMounted, ref } from 'vue'
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
} = useTwoFactor()

const code = ref('')
const disablePassword = ref('')
const showDisablePassword = ref(false)
const revealedBackupCodes = ref<string[] | null>(null)
const busy = ref(false)
const error = ref<string | null>(null)

onMounted(refresh)

async function handleStartEnrollment() {
  busy.value = true
  error.value = null
  try {
    await startEnrollment()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to start enrollment.'
  } finally {
    busy.value = false
  }
}

async function handleConfirm() {
  busy.value = true
  error.value = null
  try {
    revealedBackupCodes.value = await confirmEnrollment(code.value)
    code.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to confirm code.'
  } finally {
    busy.value = false
  }
}

async function handleRegenerateBackupCodes() {
  busy.value = true
  error.value = null
  try {
    revealedBackupCodes.value = await regenerateBackupCodes()
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to regenerate backup codes.'
  } finally {
    busy.value = false
  }
}

async function handleDisable() {
  if (!(await authStore.verifyPassword(disablePassword.value))) {
    error.value = 'Current password is incorrect.'
    return
  }
  busy.value = true
  error.value = null
  try {
    await disable()
    disablePassword.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to disable two-factor authentication.'
  } finally {
    busy.value = false
  }
}
</script>
