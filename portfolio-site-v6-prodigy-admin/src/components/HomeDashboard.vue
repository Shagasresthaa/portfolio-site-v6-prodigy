<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-3xl px-4 py-16 font-salsa">
      <div class="mb-8 flex items-center justify-between">
        <div>
          <h1 class="text-3xl">Welcome back, {{ authStore.username }}</h1>
          <p class="text-ink-muted text-sm">This dashboard is a work in progress.</p>
        </div>
        <button
          type="button"
          class="border-ink-muted/30 hover:bg-surface-muted rounded-lg border px-4 py-2 text-sm transition"
          @click="handleLogout"
        >
          Log out
        </button>
      </div>

      <div class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-8">
        <p class="text-ink-muted">
          Content management (projects, blog, highlights) will live here once the API is ready.
        </p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import AuthGate from '@/components/AuthGate.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  window.location.href = '/'
}
</script>
