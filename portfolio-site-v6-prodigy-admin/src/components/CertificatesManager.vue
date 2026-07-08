<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-5xl px-4 py-16 font-salsa">
      <a href="/home" class="text-ink-muted hover:text-ink mb-6 flex items-center gap-1 text-sm">
        <ArrowLeftIcon class="size-4" aria-hidden="true" />
        Back to Home
      </a>

      <div class="mb-8">
        <h1 class="mb-2 text-3xl">Certificates</h1>
        <p class="text-ink-muted text-sm">
          Shown in the floating certificates carousel on the public site's home page.
        </p>
      </div>

      <div class="border-ink-muted/20 bg-surface-muted/70 mb-12 flex flex-col gap-4 rounded-2xl border p-6">
        <div>
          <label class="mb-2 block text-sm font-semibold" for="title">Title (optional)</label>
          <input
            id="title"
            v-model="title"
            type="text"
            placeholder="e.g. AWS Certified Solutions Architect"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-sm focus:outline-none"
          />
        </div>

        <label
          class="border-ink-muted/30 hover:border-primary flex cursor-pointer items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 transition"
        >
          <ArrowUpTrayIcon class="size-6" aria-hidden="true" />
          <span class="text-lg font-semibold">
            {{ uploading ? 'Uploading…' : 'Click to upload certificate image' }}
          </span>
          <input type="file" accept="image/*" class="hidden" :disabled="uploading" @change="handleFileChange" />
        </label>
        <p class="text-ink-muted text-xs">Converted to WebP automatically.</p>
        <p v-if="uploadError" class="text-danger text-sm">{{ uploadError }}</p>
      </div>

      <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
      <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>

      <div v-else-if="certificates.length > 0" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        <div
          v-for="certificate in certificates"
          :key="certificate.id"
          class="border-ink-muted/20 bg-surface-muted/70 overflow-hidden rounded-2xl border"
        >
          <div class="bg-surface h-48 w-full">
            <img :src="certificate.imageUrl" :alt="certificate.title ?? ''" class="h-full w-full object-cover" />
          </div>

          <div class="p-4">
            <p v-if="certificate.title" class="mb-2 line-clamp-2 text-sm">{{ certificate.title }}</p>
            <p class="text-ink-muted mb-3 text-xs">{{ formatDate(certificate.createdAt) }}</p>

            <button
              type="button"
              class="bg-danger-soft text-danger flex w-full items-center justify-center gap-1 rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
              @click="handleDelete(certificate)"
            >
              <TrashIcon class="size-4" aria-hidden="true" />
              Delete
            </button>
          </div>
        </div>
      </div>

      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="text-ink-muted text-xl">No certificates uploaded yet</p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeftIcon, ArrowUpTrayIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { createCertificate, deleteCertificate, loadCertificates } from '@/composables/useCertificates'
import { processSingleImageUpload } from '@/composables/useImageProcessing'
import { uploadImage } from '@/composables/useImageUpload'
import type { Certificate } from '@/types/certificate'

const certificates = ref<Certificate[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)

const title = ref('')
const uploading = ref(false)
const uploadError = ref<string | null>(null)

async function refreshList() {
  loading.value = true
  loadError.value = null
  try {
    certificates.value = await loadCertificates()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load certificates.'
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
    const url = await uploadImage(blob, 'certs')
    await createCertificate(url, title.value.trim() || undefined)
    title.value = ''
    await refreshList()
  } catch (err) {
    uploadError.value = err instanceof Error ? err.message : 'Failed to upload certificate.'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleDelete(certificate: Certificate) {
  if (!confirm('Delete this certificate?')) return
  try {
    await deleteCertificate(certificate.id)
    await refreshList()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to delete certificate.'
  }
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString()
}
</script>
