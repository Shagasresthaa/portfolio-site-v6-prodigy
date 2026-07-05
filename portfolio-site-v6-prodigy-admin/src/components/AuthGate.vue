<template>
  <slot v-if="authStore.isAuthenticated" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// No session (or none yet, since this island prerenders logged-out on the
// server) - bounce straight to login rather than showing a dead page.
onMounted(() => {
  if (!authStore.isAuthenticated) window.location.href = '/'
})
</script>
