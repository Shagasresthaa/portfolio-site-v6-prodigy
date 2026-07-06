<template>
  <slot v-if="verified" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const verified = ref(false)

// Don't trust locally-cached session state alone to decide whether to render protected
// content - a tampered/stale localStorage entry (or an expired-but-not-yet-detected token)
// would otherwise get a real page shell rendered before any API call catches it. Confirm
// against the server first; only then show the slot.
onMounted(async () => {
  if (!authStore.isAuthenticated) {
    window.location.href = '/'
    return
  }
  const ok = await authStore.verifySession()
  if (!ok) {
    window.location.href = '/'
    return
  }
  verified.value = true
})
</script>
