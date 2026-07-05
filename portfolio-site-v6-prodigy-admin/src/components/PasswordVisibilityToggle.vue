<template>
  <button
    type="button"
    class="text-ink-muted hover:text-ink absolute inset-y-0 right-0 flex w-10 items-center justify-center"
    :aria-label="modelValue ? 'Hide password' : 'Show password'"
    @click="$emit('update:modelValue', !modelValue)"
  >
    <EyeSlashIcon v-if="modelValue" class="size-5" aria-hidden="true" />
    <EyeIcon v-else class="size-5" aria-hidden="true" />
  </button>
</template>

<script setup lang="ts">
import { EyeIcon, EyeSlashIcon } from '@heroicons/vue/24/outline'

defineProps<{ modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()
</script>
