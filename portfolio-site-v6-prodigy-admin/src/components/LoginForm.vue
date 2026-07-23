<template>
  <div class="mx-auto w-full max-w-md px-4 py-16">
    <form
      v-if="step === 'password'"
      class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
      @submit.prevent="handlePasswordSubmit"
    >
      <div class="text-center">
        <h1 class="mb-2 text-3xl">Admin Login</h1>
        <p class="text-ink-muted text-sm">Sign in to manage portfolio content.</p>
      </div>

      <div>
        <label class="mb-2 block text-sm font-semibold" for="username">Username</label>
        <input
          id="username"
          v-model="username"
          type="text"
          required
          autocomplete="username"
          placeholder="admin"
          class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
          @input="error = null"
        />
      </div>

      <div>
        <label class="mb-2 block text-sm font-semibold" for="password">Password</label>
        <div class="relative">
          <input
            id="password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            required
            autocomplete="current-password"
            placeholder="••••••••"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 pr-11 text-base placeholder:text-ink-muted/70 focus:outline-none"
            @input="error = null"
          />
          <PasswordVisibilityToggle v-model="showPassword" />
        </div>
      </div>

      <p v-if="error" class="text-danger -mt-2 text-sm">{{ error }}</p>

      <button
        type="submit"
        :disabled="submitting"
        class="bg-primary text-primary-contrast font-salsa rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
      >
        {{ submitting ? 'Signing in…' : 'Sign in' }}
      </button>
    </form>

    <!-- Both a security key and an authenticator app are set up - let the user pick. -->
    <div
      v-else-if="step === 'choice'"
      class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-4 rounded-2xl border p-8 shadow-xl"
    >
      <div class="text-center">
        <h1 class="mb-2 text-3xl">Verify it's you</h1>
        <p class="text-ink-muted text-sm">Choose a second factor to finish signing in.</p>
      </div>

      <button
        type="button"
        :disabled="submitting"
        class="border-ink-muted/30 hover:bg-surface flex items-center justify-center gap-2 rounded-lg border px-4 py-3 text-sm transition disabled:opacity-60"
        @click="handleChooseSecurityKey"
      >
        <FingerPrintIcon class="size-5" aria-hidden="true" />
        Use security key
      </button>
      <button
        type="button"
        :disabled="submitting"
        class="border-ink-muted/30 hover:bg-surface flex items-center justify-center gap-2 rounded-lg border px-4 py-3 text-sm transition disabled:opacity-60"
        @click="step = 'totp'"
      >
        <DevicePhoneMobileIcon class="size-5" aria-hidden="true" />
        Use authenticator app
      </button>

      <p v-if="error" class="text-danger text-sm">{{ error }}</p>

      <button type="button" class="text-ink-muted text-sm hover:opacity-80" @click="resetToPasswordStep">
        Back
      </button>
    </div>

    <!-- Either the only second factor available, or chosen from the choice step above. -->
    <form
      v-else
      class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
      @submit.prevent="handleTotpSubmit"
    >
      <div class="text-center">
        <h1 class="mb-2 text-3xl">Enter your code</h1>
        <p class="text-ink-muted text-sm">Open your authenticator app and enter the current code.</p>
      </div>

      <div>
        <label class="mb-2 block text-sm font-semibold" for="totp-code">6-digit code</label>
        <input
          id="totp-code"
          v-model="totpCode"
          type="text"
          inputmode="numeric"
          maxlength="6"
          required
          autocomplete="one-time-code"
          placeholder="000000"
          class="border-ink-muted/30 bg-surface focus:border-(--color-primary) font-salsa w-full rounded border p-3 text-base placeholder:text-ink-muted/70 focus:outline-none"
          @input="error = null"
        />
      </div>

      <p v-if="error" class="text-danger -mt-2 text-sm">{{ error }}</p>

      <button
        type="submit"
        :disabled="submitting"
        class="bg-primary text-primary-contrast font-salsa rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
      >
        {{ submitting ? 'Verifying…' : 'Verify' }}
      </button>

      <button type="button" class="text-ink-muted text-sm hover:opacity-80" @click="resetToPasswordStep">
        Back
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { DevicePhoneMobileIcon, FingerPrintIcon } from '@heroicons/vue/24/outline'
import PasswordVisibilityToggle from '@/components/PasswordVisibilityToggle.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

type Step = 'password' | 'choice' | 'totp'

const step = ref<Step>('password')
const username = ref('')
const password = ref('')
const totpCode = ref('')
const pendingWebAuthnOptions = ref<string | null>(null)
const showPassword = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

// Already authenticated - skip straight to the home page.
onMounted(() => {
  if (authStore.isAuthenticated) window.location.href = '/home'
})

function resetToPasswordStep() {
  step.value = 'password'
  password.value = ''
  totpCode.value = ''
  pendingWebAuthnOptions.value = null
  error.value = null
}

async function handlePasswordSubmit() {
  submitting.value = true
  error.value = null
  try {
    const outcome = await authStore.login(username.value.trim(), password.value)
    if (outcome.status === 'DONE') {
      window.location.href = '/home'
      return
    }
    if (outcome.status === 'TOTP_REQUIRED') {
      step.value = 'totp'
    } else {
      pendingWebAuthnOptions.value = outcome.webAuthnOptions
      step.value = 'choice'
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to sign in.'
  } finally {
    submitting.value = false
  }
}

async function handleChooseSecurityKey() {
  submitting.value = true
  error.value = null
  try {
    await authStore.loginWithSecurityKey(username.value.trim(), pendingWebAuthnOptions.value!)
    window.location.href = '/home'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to sign in.'
  } finally {
    submitting.value = false
  }
}

async function handleTotpSubmit() {
  submitting.value = true
  error.value = null
  try {
    await authStore.loginWithTotp(username.value.trim(), totpCode.value.trim())
    window.location.href = '/home'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to sign in.'
    totpCode.value = ''
  } finally {
    submitting.value = false
  }
}
</script>
