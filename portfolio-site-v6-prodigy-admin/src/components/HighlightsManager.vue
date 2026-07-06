<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-5xl px-4 py-16 font-salsa">
      <!-- List view -->
      <template v-if="view === 'list'">
        <div class="mb-8 flex items-center justify-between">
          <h1 class="text-3xl">Highlights</h1>
          <button
            type="button"
            class="bg-primary text-primary-contrast flex items-center gap-2 rounded-lg px-4 py-2 transition hover:opacity-90"
            @click="openNewForm"
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            New Moment
          </button>
        </div>

        <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
        <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>

        <div v-else-if="items.length > 0" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="item in items"
            :key="item.id"
            class="border-ink-muted/20 bg-surface-muted/70 overflow-hidden rounded-2xl border"
          >
            <div class="bg-surface relative h-48 w-full">
              <img
                v-if="item.mediaType === 'IMAGE' && item.thumbnailImage"
                :src="item.thumbnailImage"
                :alt="item.title"
                class="h-full w-full object-cover"
              />
              <img
                v-else-if="item.mediaType === 'VIDEO' && videoThumbnailUrl(item)"
                :src="videoThumbnailUrl(item)!"
                :alt="item.title"
                class="h-full w-full object-cover"
                @error="handleThumbnailError(item.id)"
              />
              <div v-else class="flex h-full items-center justify-center">
                <VideoCameraIcon class="text-ink-muted size-10" aria-hidden="true" />
              </div>
              <PlayCircleIcon
                v-if="item.mediaType === 'VIDEO' && videoThumbnailUrl(item)"
                class="absolute inset-0 m-auto size-12 text-white drop-shadow-lg"
                aria-hidden="true"
              />
            </div>

            <div class="p-4">
              <h2 class="mb-1 text-lg font-semibold">{{ item.title }}</h2>
              <p v-if="item.description" class="text-ink-muted mb-2 line-clamp-2 text-sm">
                {{ item.description }}
              </p>

              <div v-if="item.tags" class="mb-3 flex flex-wrap gap-1">
                <span
                  v-for="tag in item.tags.split(',').map((t) => t.trim())"
                  :key="tag"
                  class="bg-surface text-ink-muted rounded-full px-2 py-0.5 text-xs"
                >
                  {{ tag }}
                </span>
              </div>

              <p class="text-ink-muted mb-3 text-xs">{{ formatDate(item.createdAt) }}</p>

              <div class="flex gap-2">
                <button
                  type="button"
                  class="bg-warning-soft text-warning flex flex-1 items-center justify-center gap-1 rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="openEditForm(item)"
                >
                  <PencilIcon class="size-4" aria-hidden="true" />
                  Edit
                </button>
                <button
                  type="button"
                  class="bg-danger-soft text-danger rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="handleDelete(item)"
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
          <p class="text-ink-muted text-xl">No moments yet</p>
        </div>
      </template>

      <!-- Form view -->
      <template v-else>
        <button
          type="button"
          class="text-ink-muted hover:text-ink mb-6 flex items-center gap-1 text-sm"
          @click="view = 'list'"
        >
          <ArrowLeftIcon class="size-4" aria-hidden="true" />
          Back to moments
        </button>

        <h1 class="mb-6 text-3xl">{{ editingId ? 'Edit Moment' : 'New Moment' }}</h1>

        <form
          class="border-ink-muted/20 bg-surface-muted/70 flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
          @submit.prevent="handleSave"
        >
          <div>
            <label class="mb-2 block text-sm font-semibold" for="title">Title *</label>
            <input
              id="title"
              v-model="form.title"
              type="text"
              required
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="mediaType">Media type *</label>
            <select
              id="mediaType"
              v-model="form.mediaType"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            >
              <option value="IMAGE">Image</option>
              <option value="VIDEO">Video (YouTube)</option>
            </select>
          </div>

          <div v-if="form.mediaType === 'IMAGE'">
            <label class="mb-2 block text-sm font-semibold" for="image">Image *</label>
            <input
              id="image"
              type="file"
              accept="image/*"
              class="border-ink-muted/30 bg-surface w-full rounded border p-3 text-sm"
              @change="handleFileChange"
            />
            <p class="text-ink-muted mt-1 text-xs">
              Converted to WebP automatically, at full size and as a thumbnail.
            </p>
            <p v-if="processingImage" class="text-ink-muted mt-2 text-sm">Processing image…</p>
            <p v-if="imageError" class="text-danger mt-2 text-sm">{{ imageError }}</p>

            <div v-if="imagePreviewUrls" class="mt-4 flex gap-4">
              <div>
                <p class="text-ink-muted mb-1 text-xs">Thumbnail</p>
                <img
                  :src="imagePreviewUrls.thumbnailImage"
                  alt="Thumbnail preview"
                  class="border-ink-muted/30 h-32 w-32 rounded border object-cover"
                />
              </div>
              <div>
                <p class="text-ink-muted mb-1 text-xs">Full size</p>
                <img
                  :src="imagePreviewUrls.image"
                  alt="Full size preview"
                  class="border-ink-muted/30 h-32 w-auto rounded border object-cover"
                />
              </div>
            </div>
          </div>

          <div v-else>
            <label class="mb-2 block text-sm font-semibold" for="videoUrl">YouTube URL *</label>
            <input
              id="videoUrl"
              v-model="form.videoUrl"
              type="url"
              placeholder="https://www.youtube.com/watch?v=..."
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="description">Description</label>
            <textarea
              id="description"
              v-model="form.description"
              rows="3"
              placeholder="Optional description…"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="caption">Caption</label>
            <input
              id="caption"
              v-model="form.caption"
              type="text"
              placeholder="Optional caption…"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="tags">Tags *</label>
            <input
              id="tags"
              v-model="form.tags"
              type="text"
              required
              placeholder="hackathon, award, team"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
            <p class="text-ink-muted mt-1 text-xs">Comma-separated tags.</p>
          </div>

          <p v-if="saveError" class="text-danger text-sm">{{ saveError }}</p>

          <div class="flex gap-4">
            <button
              type="submit"
              :disabled="saving || processingImage"
              class="bg-primary text-primary-contrast rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
            >
              {{ saving ? 'Saving…' : editingId ? 'Update Moment' : 'Create Moment' }}
            </button>
            <button
              type="button"
              class="border-ink-muted/30 hover:bg-surface rounded-lg border px-8 py-3 transition"
              @click="view = 'list'"
            >
              Cancel
            </button>
          </div>
        </form>
      </template>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  ArrowLeftIcon,
  PencilIcon,
  PlusIcon,
  TrashIcon,
  VideoCameraIcon,
} from '@heroicons/vue/24/outline'
import { PlayCircleIcon } from '@heroicons/vue/24/solid'
import AuthGate from '@/components/AuthGate.vue'
import { createHighlight, deleteHighlight, loadHighlights, updateHighlight } from '@/composables/useHighlights'
import { processImageUpload, type ProcessedImage } from '@/composables/useImageProcessing'
import { uploadImage } from '@/composables/useImageUpload'
import type { HighlightItem, HighlightMediaType } from '@/types/highlight'
import { getYoutubeThumbnailUrl } from '@/utils/youtube'

const view = ref<'list' | 'form'>('list')
const items = ref<HighlightItem[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const failedThumbnails = ref<Record<string, boolean>>({})

function videoThumbnailUrl(item: HighlightItem): string | null {
  if (failedThumbnails.value[item.id] || !item.videoUrl) return null
  return getYoutubeThumbnailUrl(item.videoUrl)
}

function handleThumbnailError(id: string) {
  failedThumbnails.value = { ...failedThumbnails.value, [id]: true }
}

const editingId = ref<string | null>(null)
const form = reactive({
  title: '',
  description: '',
  caption: '',
  mediaType: 'IMAGE' as HighlightMediaType,
  videoUrl: '',
  tags: '',
})
// URLs actually rendered in <img :src>. Either the real, already-uploaded URLs (editing an
// existing item, image untouched) or local object URLs for a freshly-chosen file, in which
// case pendingImageBlobs is also set and gets uploaded on save.
const imagePreviewUrls = ref<{ image: string; thumbnailImage: string } | null>(null)
const pendingImageBlobs = ref<ProcessedImage | null>(null)
const processingImage = ref(false)
const imageError = ref<string | null>(null)
const saving = ref(false)
const saveError = ref<string | null>(null)

function revokePendingObjectUrls() {
  if (pendingImageBlobs.value && imagePreviewUrls.value) {
    URL.revokeObjectURL(imagePreviewUrls.value.image)
    URL.revokeObjectURL(imagePreviewUrls.value.thumbnailImage)
  }
}

async function refreshList() {
  loading.value = true
  loadError.value = null
  try {
    items.value = await loadHighlights()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load moments.'
  } finally {
    loading.value = false
  }
}

onMounted(refreshList)

function resetForm() {
  form.title = ''
  form.description = ''
  form.caption = ''
  form.mediaType = 'IMAGE'
  form.videoUrl = ''
  form.tags = ''
  revokePendingObjectUrls()
  imagePreviewUrls.value = null
  pendingImageBlobs.value = null
  imageError.value = null
  saveError.value = null
}

function openNewForm() {
  editingId.value = null
  resetForm()
  view.value = 'form'
}

function openEditForm(item: HighlightItem) {
  editingId.value = item.id
  form.title = item.title
  form.description = item.description ?? ''
  form.caption = item.caption ?? ''
  form.mediaType = item.mediaType
  form.videoUrl = item.videoUrl ?? ''
  form.tags = item.tags
  pendingImageBlobs.value = null
  imagePreviewUrls.value =
    item.mediaType === 'IMAGE' && item.image && item.thumbnailImage
      ? { image: item.image, thumbnailImage: item.thumbnailImage }
      : null
  imageError.value = null
  saveError.value = null
  view.value = 'form'
}

async function handleFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  processingImage.value = true
  imageError.value = null
  try {
    const processed = await processImageUpload(file)
    revokePendingObjectUrls()
    pendingImageBlobs.value = processed
    imagePreviewUrls.value = {
      image: URL.createObjectURL(processed.image),
      thumbnailImage: URL.createObjectURL(processed.thumbnailImage),
    }
  } catch (err) {
    imageError.value = err instanceof Error ? err.message : 'Failed to process image.'
  } finally {
    processingImage.value = false
  }
}

async function handleDelete(item: HighlightItem) {
  if (!confirm(`Delete "${item.title}"?`)) return
  try {
    await deleteHighlight(item.id)
    await refreshList()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to delete moment.'
  }
}

async function handleSave() {
  saveError.value = null

  if (!form.title.trim()) {
    saveError.value = 'Title is required.'
    return
  }
  if (!form.tags.trim()) {
    saveError.value = 'At least one tag is required.'
    return
  }
  if (form.mediaType === 'IMAGE' && !imagePreviewUrls.value) {
    saveError.value = 'Please upload an image.'
    return
  }
  if (form.mediaType === 'VIDEO' && !form.videoUrl.trim()) {
    saveError.value = 'Video URL is required.'
    return
  }

  saving.value = true
  try {
    let image: string | undefined
    let thumbnailImage: string | undefined

    if (form.mediaType === 'IMAGE') {
      if (pendingImageBlobs.value) {
        // A new file was chosen this session - upload it, then drop the local preview
        // (revoking its object URLs) now that the real URLs are known.
        const blobs = pendingImageBlobs.value
        ;[image, thumbnailImage] = await Promise.all([
          uploadImage(blobs.image, 'highlights'),
          uploadImage(blobs.thumbnailImage, 'highlights'),
        ])
        revokePendingObjectUrls()
        pendingImageBlobs.value = null
        imagePreviewUrls.value = { image, thumbnailImage }
      } else {
        // Editing an existing item without touching its image - reuse the real URLs already
        // shown in the preview.
        image = imagePreviewUrls.value!.image
        thumbnailImage = imagePreviewUrls.value!.thumbnailImage
      }
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      caption: form.caption.trim() || undefined,
      mediaType: form.mediaType,
      tags: form.tags.trim(),
      ...(form.mediaType === 'IMAGE' ? { image, thumbnailImage } : { videoUrl: form.videoUrl.trim() }),
    }

    if (editingId.value) {
      await updateHighlight(editingId.value, payload)
    } else {
      await createHighlight(payload)
    }

    await refreshList()
    view.value = 'list'
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : 'Failed to save moment.'
  } finally {
    saving.value = false
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}
</script>
