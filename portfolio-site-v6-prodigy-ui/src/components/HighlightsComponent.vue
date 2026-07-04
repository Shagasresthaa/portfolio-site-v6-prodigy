<template>
  <div class="mx-auto w-full max-w-6xl px-4 py-12">
    <div class="mb-12 text-center">
      <h1 class="font-salsa text-primary mb-3 text-4xl font-bold md:text-5xl">Highlights</h1>
      <p class="font-kalam text-ink-muted text-lg md:text-xl">
        A collection of highlights, demos, and achievements
      </p>
    </div>

    <template v-if="highlightsContent">
      <!-- Tag filter -->
      <div class="mb-8 flex flex-wrap items-center gap-2">
        <TagFilterPopover
          v-model="appliedTags"
          :all-values="allTags"
          label="Filter by Tags"
          panel-title="Select Tags"
          search-placeholder="Search tags..."
        />
      </div>

      <!-- Grid -->
      <!-- items-start (not stretch) + a JS-measured cardMinHeight (floor, not a live
      stretch) gets the best of both: cards read as a uniform grid, but expanding one
      card's description can still grow past that floor without forcing its row-mates
      to grow too - see recalcCardMinHeight, which only reruns on a new set of cards
      (page/filter change) or resize, never on a card's own expand/collapse. -->
      <div v-if="pagedItems.length > 0" class="grid grid-cols-1 items-start gap-6 md:grid-cols-2 lg:grid-cols-3">
        <HighlightCard
          v-for="item in pagedItems"
          :key="item.id"
          :ref="(el) => setCardRef(item.id, el)"
          :item="item"
          :style="cardMinHeight > 0 ? { minHeight: `${cardMinHeight}px` } : undefined"
          @view="openModal(item)"
        />
      </div>
      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="font-kalam text-ink-muted text-lg">
          {{ appliedTags.length > 0 ? 'No highlights found with selected tags' : 'No highlights yet' }}
        </p>
        <button
          v-if="appliedTags.length > 0"
          type="button"
          class="text-primary font-salsa mt-4 hover:opacity-80"
          @click="appliedTags = []"
        >
          Clear filters
        </button>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="font-salsa mt-12 flex items-center justify-center gap-4">
        <button
          type="button"
          class="bg-ink/5 hover:bg-ink/10 text-ink flex items-center gap-2 rounded-lg px-4 py-2 backdrop-blur-md transition-colors disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="page === 1"
          @click="page = Math.max(1, page - 1)"
        >
          <FontAwesomeIcon :icon="faChevronLeft" class="size-3" aria-hidden="true" />
          Previous
        </button>

        <div class="flex items-center gap-2">
          <button
            v-for="pageNum in totalPages"
            :key="pageNum"
            type="button"
            class="size-10 rounded-lg transition-colors"
            :class="page === pageNum ? 'bg-primary text-primary-contrast' : 'bg-ink/5 text-ink-muted hover:bg-ink/10'"
            @click="page = pageNum"
          >
            {{ pageNum }}
          </button>
        </div>

        <button
          type="button"
          class="bg-ink/5 hover:bg-ink/10 text-ink flex items-center gap-2 rounded-lg px-4 py-2 backdrop-blur-md transition-colors disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="page === totalPages"
          @click="page = Math.min(totalPages, page + 1)"
        >
          Next
          <FontAwesomeIcon :icon="faChevronRight" class="size-3" aria-hidden="true" />
        </button>
      </div>
    </template>

    <p v-else class="text-ink-muted text-center">Loading...</p>

    <!-- Image modal - thumbnails in the grid are low-res stand-ins, this shows item.image (the actual/full image) -->
    <TransitionRoot :show="selectedItem !== null" as="template">
      <Dialog as="div" class="relative z-50" @close="selectedItem = null">
        <TransitionChild
          as="template"
          enter="duration-200 ease-out"
          enter-from="opacity-0"
          enter-to="opacity-100"
          leave="duration-150 ease-in"
          leave-from="opacity-100"
          leave-to="opacity-0"
        >
          <div class="bg-ink/70 fixed inset-0 backdrop-blur-sm" />
        </TransitionChild>

        <div class="fixed inset-0 flex items-center justify-center p-4">
          <TransitionChild
            as="template"
            enter="duration-200 ease-out"
            enter-from="scale-95 opacity-0"
            enter-to="scale-100 opacity-100"
            leave="duration-150 ease-in"
            leave-from="scale-100 opacity-100"
            leave-to="scale-95 opacity-0"
          >
            <DialogPanel class="bg-surface relative w-full max-w-4xl overflow-hidden rounded-2xl shadow-2xl">
              <button
                type="button"
                class="bg-ink/50 hover:bg-ink/70 text-surface absolute top-4 right-4 z-10 rounded-full p-2 transition-colors"
                aria-label="Close"
                @click="selectedItem = null"
              >
                <FontAwesomeIcon :icon="faXmark" class="size-5" aria-hidden="true" />
              </button>

              <img
                v-if="selectedItem"
                :src="selectedItem.image"
                :alt="selectedItem.title"
                class="max-h-[70vh] w-full object-contain"
              />

              <div v-if="selectedItem?.caption" class="border-ink-muted/20 bg-surface-muted border-t p-4 text-center">
                <p class="font-kalam text-ink-muted text-sm italic">"{{ selectedItem.caption }}"</p>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </Dialog>
    </TransitionRoot>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Dialog, DialogPanel, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faChevronLeft, faChevronRight, faXmark } from '@fortawesome/free-solid-svg-icons'
import { useCachedResource } from '@/composables/useCachedResource'
import { useUniformCardHeight } from '@/composables/useUniformCardHeight'
import HighlightCard from './HighlightCard.vue'
import TagFilterPopover from './TagFilterPopover.vue'
import type { HighlightItem } from '@/types/highlight'

interface HighlightsContent {
  items: HighlightItem[]
}

const { data: highlightsContent } = useCachedResource<HighlightsContent>(
  'highlights-content',
  '/data/highlights.json',
)

const ITEMS_PER_PAGE = 12

const page = ref(1)
const appliedTags = ref<string[]>([])
const selectedItem = ref<HighlightItem | null>(null)

function parseTags(tags: string): string[] {
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const sortedItems = computed(() => {
  const items = highlightsContent.value?.items ?? []
  return [...items].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
})

const allTags = computed(() => {
  const tagSet = new Set<string>()
  sortedItems.value.forEach((item) => parseTags(item.tags).forEach((tag) => tagSet.add(tag)))
  return Array.from(tagSet).sort()
})

const filteredItems = computed(() => {
  if (appliedTags.value.length === 0) return sortedItems.value
  return sortedItems.value.filter((item) => {
    const itemTags = parseTags(item.tags)
    return appliedTags.value.every((tag) => itemTags.includes(tag))
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / ITEMS_PER_PAGE)))

const pagedItems = computed(() => {
  const start = (page.value - 1) * ITEMS_PER_PAGE
  return filteredItems.value.slice(start, start + ITEMS_PER_PAGE)
})

watch(appliedTags, () => {
  page.value = 1
})

const { cardMinHeight, setCardRef } = useUniformCardHeight(() => pagedItems.value)

function openModal(item: HighlightItem) {
  selectedItem.value = item
}
</script>
