<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-4xl px-4 py-16 font-salsa">
      <div class="mb-8">
        <h1 class="text-3xl">Welcome back, {{ authStore.username }}</h1>
        <p class="text-ink-muted text-sm">
          Manage the content shown on the public site's home page.
        </p>
      </div>

      <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
      <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>
      <p v-else-if="!draft" class="text-danger text-center">No content loaded.</p>

      <form v-else class="flex flex-col gap-8" @submit.prevent="handleSave">
        <!-- Resume -->
        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <h2 class="mb-3 text-xl">Resume</h2>
          <p v-if="draft.resumeUrl" class="text-ink-muted mb-3 text-sm">
            Current:
            <a
              :href="draft.resumeUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-primary underline"
            >
              View PDF
            </a>
          </p>
          <p v-else class="text-ink-muted mb-3 text-sm">No resume uploaded yet.</p>
          <input
            ref="resumeFileInput"
            type="file"
            accept="application/pdf"
            class="hidden"
            @change="handleResumeFileChange"
          />
          <div class="flex items-center gap-4">
            <button
              type="button"
              class="border-ink-muted/30 hover:bg-surface flex items-center gap-1 rounded-lg border px-3 py-2 text-sm transition disabled:opacity-60"
              :disabled="uploadingResume"
              @click="resumeFileInput?.click()"
            >
              {{ uploadingResume ? 'Uploading…' : 'Upload new resume' }}
            </button>
            <p v-if="resumeUploadError" class="text-danger text-sm">{{ resumeUploadError }}</p>
          </div>
        </section>

        <!-- About hook -->
        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <h2 class="mb-3 text-xl">About hook</h2>
          <textarea
            v-model="draft.aboutHook"
            rows="3"
            class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
          />
        </section>

        <!-- About story -->
        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <h2 class="mb-3 text-xl">About - story paragraphs</h2>
          <div class="flex flex-col gap-3">
            <div v-for="(_, index) in draft.aboutStory" :key="index" class="flex gap-2">
              <textarea
                v-model="draft.aboutStory[index]"
                rows="3"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
              <div class="flex flex-col gap-1">
                <button
                  type="button"
                  class="text-ink-muted hover:text-ink disabled:opacity-30"
                  :disabled="index === 0"
                  title="Move up"
                  @click="move(draft.aboutStory, index, -1)"
                >
                  <ChevronUpIcon class="size-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  class="text-ink-muted hover:text-ink disabled:opacity-30"
                  :disabled="index === draft.aboutStory.length - 1"
                  title="Move down"
                  @click="move(draft.aboutStory, index, 1)"
                >
                  <ChevronDownIcon class="size-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  class="text-danger hover:opacity-80"
                  title="Remove paragraph"
                  @click="draft.aboutStory.splice(index, 1)"
                >
                  <TrashIcon class="size-4" aria-hidden="true" />
                </button>
              </div>
            </div>
          </div>
          <button
            type="button"
            class="border-ink-muted/30 hover:bg-surface mt-3 flex items-center gap-1 rounded-lg border px-3 py-2 text-sm transition"
            @click="draft.aboutStory.push('')"
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            Add paragraph
          </button>
        </section>

        <!-- Timeline -->
        <section class="border-ink-muted/20 bg-surface-muted/70 rounded-2xl border p-6">
          <h2 class="mb-3 text-xl">Career timeline</h2>
          <div class="flex flex-col gap-4">
            <div
              v-for="(event, index) in draft.timeline"
              :key="index"
              class="border-ink-muted/20 bg-surface rounded-lg border p-4"
            >
              <div class="mb-3 flex items-center justify-between">
                <p class="text-ink-muted text-xs">Event {{ index + 1 }}</p>
                <div class="flex gap-2">
                  <button
                    type="button"
                    class="text-ink-muted hover:text-ink disabled:opacity-30"
                    :disabled="index === 0"
                    title="Move up"
                    @click="move(draft.timeline, index, -1)"
                  >
                    <ChevronUpIcon class="size-4" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    class="text-ink-muted hover:text-ink disabled:opacity-30"
                    :disabled="index === draft.timeline.length - 1"
                    title="Move down"
                    @click="move(draft.timeline, index, 1)"
                  >
                    <ChevronDownIcon class="size-4" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    class="text-danger hover:opacity-80"
                    title="Remove event"
                    @click="draft.timeline.splice(index, 1)"
                  >
                    <TrashIcon class="size-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
              <div class="grid gap-3 sm:grid-cols-2">
                <input
                  v-model="event.title"
                  type="text"
                  placeholder="Title"
                  class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) rounded border p-2 text-sm focus:outline-none"
                />
                <input
                  v-model="event.position"
                  type="text"
                  placeholder="Position"
                  class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) rounded border p-2 text-sm focus:outline-none"
                />
                <input
                  v-model="event.institution"
                  type="text"
                  placeholder="Institution"
                  class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) rounded border p-2 text-sm focus:outline-none"
                />
                <input
                  v-model="event.date"
                  type="text"
                  placeholder="Date (e.g. Jan, 2024 - May, 2026)"
                  class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) rounded border p-2 text-sm focus:outline-none"
                />
                <input
                  v-model="event.duration"
                  type="text"
                  placeholder="Duration (optional)"
                  class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) rounded border p-2 text-sm focus:outline-none sm:col-span-2"
                />
              </div>
            </div>
          </div>
          <button
            type="button"
            class="border-ink-muted/30 hover:bg-surface mt-3 flex items-center gap-1 rounded-lg border px-3 py-2 text-sm transition"
            @click="
              draft.timeline.push({ title: '', position: '', institution: '', date: '', duration: '' })
            "
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            Add timeline event
          </button>
        </section>

        <div class="flex items-center gap-4">
          <button
            type="submit"
            :disabled="saving"
            class="bg-primary text-primary-contrast rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
          >
            {{ saving ? 'Saving…' : 'Save changes' }}
          </button>
          <p v-if="saved" class="text-secondary text-sm">Saved.</p>
          <p v-if="saveError" class="text-danger text-sm">{{ saveError }}</p>
        </div>
      </form>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ChevronDownIcon, ChevronUpIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { useAuthStore } from '@/stores/auth'
import { fetchHomeContent, saveHomeContent, type HomeContent } from '@/composables/useHomeContent'
import { uploadDocument } from '@/composables/useDocumentUpload'

const authStore = useAuthStore()

const draft = ref<HomeContent | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const saving = ref(false)
const saved = ref(false)
const saveError = ref<string | null>(null)

const resumeFileInput = ref<HTMLInputElement | null>(null)
const uploadingResume = ref(false)
const resumeUploadError = ref<string | null>(null)

onMounted(async () => {
  try {
    draft.value = await fetchHomeContent()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load content.'
  } finally {
    loading.value = false
  }
})

// Clear the "Saved." message as soon as the draft changes again, so it can't
// linger and imply an edit is saved when it isn't.
watch(draft, () => (saved.value = false), { deep: true })

async function handleResumeFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !draft.value) return

  uploadingResume.value = true
  resumeUploadError.value = null
  try {
    draft.value.resumeUrl = await uploadDocument(file, 'resume')
  } catch (err) {
    resumeUploadError.value = err instanceof Error ? err.message : 'Failed to upload resume.'
  } finally {
    uploadingResume.value = false
    input.value = ''
  }
}

function move<T>(list: T[], index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item as T)
}

async function handleSave() {
  if (!draft.value) return
  saving.value = true
  saveError.value = null
  try {
    // Not reassigning draft from the response - it's just an echo of what was sent, and doing
    // so would re-trigger the deep watch above, immediately flipping `saved` back to false.
    await saveHomeContent(draft.value)
    saved.value = true
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : 'Failed to save changes.'
  } finally {
    saving.value = false
  }
}

</script>
