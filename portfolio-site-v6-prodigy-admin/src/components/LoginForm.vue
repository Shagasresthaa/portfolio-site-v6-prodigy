<template>
  <div class="mx-auto w-full max-w-md px-4 py-16">
    <form
      class="border-ink-muted/20 bg-surface-muted/70 font-salsa flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
      @submit.prevent="handleSubmit"
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PasswordVisibilityToggle from '@/components/PasswordVisibilityToggle.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

// Already have a valid session (e.g. came back to `/` after logging in) -
// skip straight to the home page rather than showing the form again.
onMounted(() => {
  if (authStore.isAuthenticated) window.location.href = '/home'
})

async function handleSubmit() {
  submitting.value = true
  error.value = null
  try {
    await authStore.login(username.value.trim(), password.value)
    window.location.href = '/home'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to sign in.'
    submitting.value = false
  }
}
</script>
