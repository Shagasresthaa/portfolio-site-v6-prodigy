<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-5xl px-4 py-16 font-salsa">
      <a href="/blog" class="text-ink-muted hover:text-ink mb-6 flex items-center gap-1 text-sm">
        <ArrowLeftIcon class="size-4" aria-hidden="true" />
        Back to Blog
      </a>

      <div class="mb-8">
        <h1 class="mb-2 text-3xl">Blog Images</h1>
        <p class="text-ink-muted text-sm">
          Upload images here, then copy a URL and paste it into a post's markdown
          (<code>![alt text](url)</code>) to reuse it across posts.
        </p>
      </div>

      <div class="border-ink-muted/20 bg-surface-muted/70 mb-12 flex flex-col gap-4 rounded-2xl border p-6">
        <div>
          <label class="mb-2 block text-sm font-semibold" for="altText">Alt text (optional)</label>
          <input
            id="altText"
            v-model="altText"
            type="text"
            placeholder="Describe the image…"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-sm focus:outline-none"
          />
        </div>

        <label
          class="border-ink-muted/30 hover:border-primary flex cursor-pointer items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 transition"
        >
          <ArrowUpTrayIcon class="size-6" aria-hidden="true" />
          <span class="text-lg font-semibold">
            {{ uploading ? 'Uploading…' : 'Click to upload image' }}
          </span>
          <input type="file" accept="image/*" class="hidden" :disabled="uploading" @change="handleFileChange" />
        </label>
        <p class="text-ink-muted text-xs">Converted to WebP automatically.</p>
        <p v-if="uploadError" class="text-danger text-sm">{{ uploadError }}</p>
      </div>

      <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
      <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>

      <div v-else-if="images.length > 0" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="image in images"
          :key="image.id"
          class="border-ink-muted/20 bg-surface-muted/70 overflow-hidden rounded-2xl border"
        >
          <div class="bg-surface h-48 w-full">
            <img :src="image.url" :alt="image.altText ?? ''" class="h-full w-full object-cover" />
          </div>

          <div class="p-4">
            <p v-if="image.altText" class="text-ink-muted mb-2 line-clamp-2 text-sm">{{ image.altText }}</p>
            <p class="text-ink-muted mb-3 text-xs">{{ formatDate(image.createdAt) }}</p>

            <div class="flex gap-2">
              <button
                type="button"
                class="bg-primary/20 text-primary flex flex-1 items-center justify-center gap-1 rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                @click="handleCopy(image)"
              >
                <CheckIcon v-if="copiedId === image.id" class="size-4" aria-hidden="true" />
                <ClipboardIcon v-else class="size-4" aria-hidden="true" />
                {{ copiedId === image.id ? 'Copied!' : 'Copy URL' }}
              </button>
              <button
                type="button"
                class="bg-danger-soft text-danger rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                @click="handleDelete(image)"
              >
                <TrashIcon class="size-4" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="text-ink-muted text-xl">No images uploaded yet</p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeftIcon, ArrowUpTrayIcon, CheckIcon, ClipboardIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { createBlogImage, deleteBlogImage, loadBlogImages } from '@/composables/useBlogImages'
import { processSingleImageUpload } from '@/composables/useImageProcessing'
import { uploadImage } from '@/composables/useImageUpload'
import type { BlogImage } from '@/types/blogImage'

const images = ref<BlogImage[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)

const altText = ref('')
const uploading = ref(false)
const uploadError = ref<string | null>(null)

const copiedId = ref<string | null>(null)

async function refreshList() {
  loading.value = true
  loadError.value = null
  try {
    images.value = await loadBlogImages()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load images.'
  } finally {
    loading.value = false
  }
}

onMounted(refreshList)

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  uploadError.value = null
  try {
    const blob = await processSingleImageUpload(file)
    const url = await uploadImage(blob, 'blog')
    await createBlogImage(url, altText.value.trim() || undefined)
    altText.value = ''
    await refreshList()
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to upload image.'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleCopy(image: BlogImage) {
  await navigator.clipboard.writeText(image.url)
  copiedId.value = image.id
  setTimeout(() => {
    if (copiedId.value === image.id) copiedId.value = null
  }, 2000)
}

async function handleDelete(image: BlogImage) {
  if (!confirm('Delete this image?')) return
  try {
    await deleteBlogImage(image.id)
    await refreshList()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to delete image.'
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}
</script>
