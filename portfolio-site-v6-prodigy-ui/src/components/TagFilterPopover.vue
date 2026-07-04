<template>
  <Popover v-slot="{ close }" class="relative">
    <PopoverButton
      class="bg-ink/5 hover:bg-ink/10 text-ink font-salsa flex items-center gap-2 rounded-lg px-4 py-2 backdrop-blur-md transition-colors"
      @click="pending = [...modelValue]"
    >
      <FontAwesomeIcon :icon="faFilter" class="size-4" aria-hidden="true" />
      {{ label }}
      <span v-if="modelValue.length > 0" class="bg-primary text-primary-contrast rounded-full px-2 py-0.5 text-xs">
        {{ modelValue.length }}
      </span>
    </PopoverButton>

    <PopoverPanel
      class="border-ink-muted/20 bg-surface font-salsa absolute top-full left-0 z-50 mt-2 w-80 rounded-2xl border shadow-xl backdrop-blur-md"
    >
      <div class="border-ink-muted/20 flex items-center justify-between border-b px-4 py-3">
        <span class="text-sm font-semibold">{{ panelTitle }}</span>
        <PopoverButton class="text-ink-muted hover:text-ink transition-colors" aria-label="Close">
          <FontAwesomeIcon :icon="faXmark" class="size-4" aria-hidden="true" />
        </PopoverButton>
      </div>

      <div v-if="pending.length > 0" class="border-ink-muted/20 border-b p-3">
        <div class="flex flex-wrap gap-2">
          <span
            v-for="value in pending"
            :key="value"
            class="bg-primary/20 text-primary flex items-center gap-1 rounded-full px-3 py-1 text-xs"
          >
            {{ value }}
            <button
              type="button"
              class="hover:text-danger"
              :aria-label="`Remove ${value}`"
              @click="togglePending(value)"
            >
              <FontAwesomeIcon :icon="faXmark" class="size-3" aria-hidden="true" />
            </button>
          </span>
        </div>
      </div>

      <div class="border-ink-muted/20 border-b p-3">
        <div class="relative">
          <FontAwesomeIcon
            :icon="faMagnifyingGlass"
            class="text-ink-muted/70 absolute top-1/2 left-3 -translate-y-1/2 text-sm"
            aria-hidden="true"
          />
          <input
            v-model="search"
            type="text"
            :placeholder="searchPlaceholder"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded-lg border py-2 pr-3 pl-9 text-sm focus:outline-none"
          />
        </div>
      </div>

      <div class="max-h-48 overflow-y-auto p-2">
        <button
          v-for="value in filteredAvailableValues"
          :key="value"
          type="button"
          class="hover:bg-ink/5 w-full rounded-lg px-4 py-2 text-left text-sm transition-colors"
          @click="togglePending(value)"
        >
          {{ value }}
        </button>
        <p v-if="filteredAvailableValues.length === 0" class="text-ink-muted py-4 text-center text-sm">
          {{ pending.length > 0 ? 'All options selected' : 'No options found' }}
        </p>
      </div>

      <div class="border-ink-muted/20 space-y-2 border-t p-3">
        <button
          type="button"
          class="bg-secondary text-secondary-contrast w-full rounded-lg px-4 py-2 text-sm transition hover:opacity-90"
          @click="apply(close)"
        >
          Apply Filters<template v-if="pending.length > 0"> ({{ pending.length }})</template>
        </button>
        <button
          v-if="pending.length > 0"
          type="button"
          class="bg-ink/5 text-ink-muted hover:bg-ink/10 w-full rounded-lg px-4 py-2 text-sm transition-colors"
          @click="pending = []"
        >
          Clear Selection
        </button>
      </div>
    </PopoverPanel>
  </Popover>

  <span
    v-for="value in modelValue"
    :key="value"
    class="bg-primary/20 text-primary font-salsa flex items-center gap-2 rounded-full px-3 py-1 text-sm"
  >
    {{ value }}
    <button type="button" class="hover:text-danger" :aria-label="`Remove ${value}`" @click="removeApplied(value)">
      <FontAwesomeIcon :icon="faXmark" class="size-3" aria-hidden="true" />
    </button>
  </span>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Popover, PopoverButton, PopoverPanel } from '@headlessui/vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faFilter, faMagnifyingGlass, faXmark } from '@fortawesome/free-solid-svg-icons'

const props = defineProps<{
  modelValue: string[]
  allValues: string[]
  label: string
  panelTitle: string
  searchPlaceholder: string
}>()

const emit = defineEmits<{ 'update:modelValue': [string[]] }>()

const pending = ref<string[]>([])
const search = ref('')

const filteredAvailableValues = computed(() => {
  const query = search.value.trim().toLowerCase()
  return props.allValues.filter(
    (value) => !pending.value.includes(value) && (query === '' || value.toLowerCase().includes(query)),
  )
})

function togglePending(value: string) {
  const index = pending.value.indexOf(value)
  if (index === -1) pending.value.push(value)
  else pending.value.splice(index, 1)
}

function removeApplied(value: string) {
  emit(
    'update:modelValue',
    props.modelValue.filter((v) => v !== value),
  )
}

function apply(close: () => void) {
  emit('update:modelValue', [...pending.value])
  close()
}
</script>
