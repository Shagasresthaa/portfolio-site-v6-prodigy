<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-3xl px-4 py-16 font-salsa">
      <div class="mb-8">
        <h1 class="text-3xl">Site Settings</h1>
        <p class="text-ink-muted text-sm">
          Site-wide SEO defaults - used on pages without their own content (Home, Projects,
          Highlights, Contact). Blog posts use their own title/excerpt/cover image directly instead.
        </p>
      </div>

      <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
      <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>
      <p v-else-if="!draft" class="text-danger text-center">No settings loaded.</p>

      <form v-else class="flex flex-col gap-6" @submit.prevent="handleSave">
        <div class="flex items-center justify-between">
          <p class="text-ink-muted text-xs">
            {{ hasDraft ? 'Showing your saved draft.' : 'Showing default settings.' }}
          </p>
          <button
            v-if="hasDraft"
            type="button"
            class="text-danger text-xs hover:opacity-80"
            @click="handleReset"
          >
            Reset to defaults
          </button>
        </div>

        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <label class="mb-2 block text-sm font-semibold" for="siteTitle">Site title / tagline</label>
          <input
            id="siteTitle"
            v-model="draft.siteTitle"
            type="text"
            required
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
          />
          <p class="text-ink-muted mt-1 text-xs">
            Shown as the tab-title suffix on every page, e.g. "Blog - {{ draft.siteTitle || '…' }}".
          </p>
        </section>

        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <label class="mb-2 block text-sm font-semibold" for="defaultDescription"
            >Default social description</label
          >
          <textarea
            id="defaultDescription"
            v-model="draft.defaultDescription"
            required
            rows="3"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
          />
        </section>

        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <label class="mb-2 block text-sm font-semibold" for="shareImage"
            >Default social share image</label
          >
          <input
            id="shareImage"
            type="file"
            accept="image/*"
            class="border-ink-muted/30 bg-surface w-full rounded border p-3 text-sm"
            @change="handleFileChange"
          />
          <p class="text-ink-muted mt-1 text-xs">Converted to WebP automatically.</p>
          <p v-if="processingImage" class="text-ink-muted mt-2 text-sm">Processing image…</p>
          <p v-if="imageError" class="text-danger mt-2 text-sm">{{ imageError }}</p>
          <img
            v-if="draft.defaultShareImage"
            :src="draft.defaultShareImage"
            alt="Default share image preview"
            class="border-ink-muted/30 mt-4 h-32 w-auto rounded border object-cover"
          />
        </section>

        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <label class="flex items-center gap-3">
            <input
              v-model="draft.searchIndexingEnabled"
              type="checkbox"
              class="accent-(--color-primary) h-5 w-5 rounded"
            />
            <span class="font-semibold">Allow search engines to index this site</span>
          </label>
          <p v-if="!draft.searchIndexingEnabled" class="bg-warning-soft text-warning mt-3 rounded-lg p-3 text-sm">
            Every page will emit <code>noindex, nofollow</code> - search engines will be blocked from
            indexing the whole site. Only meant for dev/staging.
          </p>
        </section>

        <div class="flex items-center gap-4">
          <button
            type="submit"
            class="bg-primary text-primary-contrast rounded-lg px-8 py-3 transition hover:opacity-90"
          >
            Save changes
          </button>
          <p v-if="saved" class="text-secondary text-sm">Saved.</p>
        </div>
      </form>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AuthGate from '@/components/AuthGate.vue'
import {
  clearSiteSettingsOverride,
  fetchBaseSiteSettings,
  loadSiteSettings,
  saveSiteSettingsOverride,
} from '@/composables/useSiteSettings'
import { processSingleImageUpload } from '@/composables/useImageProcessing'
import type { SiteSettings } from '@/types/siteSettings'

const draft = ref<SiteSettings | null>(null)
const hasDraft = ref(false)
const loading = ref(true)
const loadError = ref<string | null>(null)
const saved = ref(false)
const processingImage = ref(false)
const imageError = ref<string | null>(null)

onMounted(async () => {
  try {
    const result = await loadSiteSettings()
    draft.value = result.settings
    hasDraft.value = result.isDraft
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load settings.'
  } finally {
    loading.value = false
  }
})

watch(draft, () => (saved.value = false), { deep: true })

async function handleFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !draft.value) return

  processingImage.value = true
  imageError.value = null
  try {
    draft.value.defaultShareImage = await processSingleImageUpload(file)
  } catch (err) {
    imageError.value = err instanceof Error ? err.message : 'Failed to process image.'
  } finally {
    processingImage.value = false
  }
}

function handleSave() {
  if (!draft.value) return
  saveSiteSettingsOverride(draft.value)
  hasDraft.value = true
  saved.value = true
}

async function handleReset() {
  clearSiteSettingsOverride()
  hasDraft.value = false
  saved.value = false
  loading.value = true
  loadError.value = null
  try {
    draft.value = await fetchBaseSiteSettings()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load settings.'
  } finally {
    loading.value = false
  }
}
</script>
