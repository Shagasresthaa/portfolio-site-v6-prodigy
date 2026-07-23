<template>
  <slot v-if="verified" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const verified = ref(false)

// Confirm against the server before showing the slot - a stale/tampered localStorage token
// alone shouldn't be enough to render protected content.
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
