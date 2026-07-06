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
        <div class="flex items-center justify-between">
          <p class="text-ink-muted text-xs">
            {{ hasDraft ? 'Showing your saved draft.' : 'Showing default content.' }}
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
import { ChevronDownIcon, ChevronUpIcon, PlusIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { useAuthStore } from '@/stores/auth'
import {
  clearHomeOverride,
  fetchBaseHomeContent,
  loadHomeContent,
  saveHomeOverride,
  type HomeContent,
} from '@/composables/useHomeContent'

const authStore = useAuthStore()

const draft = ref<HomeContent | null>(null)
const hasDraft = ref(false)
const loading = ref(true)
const loadError = ref<string | null>(null)
const saved = ref(false)

onMounted(async () => {
  try {
    const result = await loadHomeContent()
    draft.value = result.content
    hasDraft.value = result.isDraft
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load content.'
  } finally {
    loading.value = false
  }
})

// Clear the "Saved." message as soon as the draft changes again, so it can't
// linger and imply an edit is saved when it isn't.
watch(draft, () => (saved.value = false), { deep: true })

function move<T>(list: T[], index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= list.length) return
  const [item] = list.splice(index, 1)
  list.splice(target, 0, item as T)
}

function handleSave() {
  if (!draft.value) return
  saveHomeOverride(draft.value)
  hasDraft.value = true
  saved.value = true
}

async function handleReset() {
  clearHomeOverride()
  hasDraft.value = false
  saved.value = false
  loading.value = true
  loadError.value = null
  try {
    draft.value = await fetchBaseHomeContent()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load content.'
  } finally {
    loading.value = false
  }
}

</script>
