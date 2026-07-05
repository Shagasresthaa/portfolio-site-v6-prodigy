<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-3xl px-4 py-16 font-salsa text-center">
      <h1 class="mb-3 text-3xl">{{ title }}</h1>
      <p class="text-ink-muted">{{ description }}</p>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import AuthGate from '@/components/AuthGate.vue'

defineProps<{ title: string; description: string }>()
</script>
