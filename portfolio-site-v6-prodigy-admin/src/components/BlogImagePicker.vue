<template>
  <Disclosure v-slot="{ open }" as="div" class="border-ink-muted/20 bg-surface rounded-lg border">
    <DisclosureButton class="flex w-full items-center justify-between px-4 py-3 text-sm font-semibold">
      <span class="flex items-center gap-2">
        <PhotoIcon class="size-4" aria-hidden="true" />
        Image Library
      </span>
      <ChevronDownIcon
        :class="open ? 'rotate-180' : ''"
        class="size-4 transition-transform"
        aria-hidden="true"
      />
    </DisclosureButton>

    <DisclosurePanel class="border-ink-muted/20 border-t p-4">
      <p v-if="loading" class="text-ink-muted text-sm">Loading…</p>
      <p v-else-if="loadError" class="text-danger text-sm">{{ loadError }}</p>
      <p v-else-if="images.length === 0" class="text-ink-muted text-sm">
        No images yet - <a href="/blog/images" target="_blank" class="text-primary hover:underline">upload some</a>.
      </p>

      <div v-else class="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        <div
          v-for="image in images"
          :key="image.id"
          class="group border-ink-muted/20 relative aspect-square overflow-hidden rounded-lg border"
        >
          <button
            type="button"
            class="h-full w-full"
            :title="image.altText || 'Insert into post'"
            @click="$emit('select', image)"
          >
            <img :src="image.url" :alt="image.altText ?? ''" class="h-full w-full object-cover" />
          </button>

          <span
            class="bg-ink/60 pointer-events-none absolute inset-x-0 bottom-0 flex items-center justify-center gap-1 py-1 text-xs text-white opacity-0 transition-opacity group-hover:opacity-100"
          >
            <PlusIcon class="size-3" aria-hidden="true" />
            Insert
          </span>

          <button
            type="button"
            class="bg-ink/60 absolute top-1 right-1 rounded p-1 text-white opacity-0 transition-opacity group-hover:opacity-100"
            title="Copy URL"
            @click="handleCopy(image)"
          >
            <CheckIcon v-if="copiedId === image.id" class="size-3" aria-hidden="true" />
            <ClipboardIcon v-else class="size-3" aria-hidden="true" />
          </button>
        </div>
      </div>

      <a href="/blog/images" target="_blank" class="text-primary mt-3 inline-block text-xs hover:underline">
        Manage images →
      </a>
    </DisclosurePanel>
  </Disclosure>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue'
import { CheckIcon, ChevronDownIcon, ClipboardIcon, PhotoIcon, PlusIcon } from '@heroicons/vue/24/outline'
import { loadBlogImages } from '@/composables/useBlogImages'
import type { BlogImage } from '@/types/blogImage'

defineEmits<{ select: [image: BlogImage] }>()

const images = ref<BlogImage[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const copiedId = ref<string | null>(null)

onMounted(async () => {
  try {
    images.value = await loadBlogImages()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load images.'
  } finally {
    loading.value = false
  }
})

async function handleCopy(image: BlogImage) {
  await navigator.clipboard.writeText(image.url)
  copiedId.value = image.id
  setTimeout(() => {
    if (copiedId.value === image.id) copiedId.value = null
  }, 2000)
}
</script>
