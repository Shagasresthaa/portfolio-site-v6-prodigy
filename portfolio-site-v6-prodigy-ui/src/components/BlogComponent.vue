<template>
  <div class="container mx-auto px-4 py-12">
    <div class="mb-12 text-center">
      <h1 class="font-salsa text-primary mb-3 text-4xl font-bold md:text-5xl">Blog</h1>
      <p class="font-kalam text-ink-muted text-lg md:text-xl">Writeups on projects, postmortems, and things I learned</p>
    </div>

    <template v-if="blogContent">
      <div class="mb-8 space-y-4">
        <!-- Search -->
        <div class="font-salsa flex gap-2">
          <div class="relative flex-1">
            <FontAwesomeIcon
              :icon="faMagnifyingGlass"
              class="text-ink-muted/70 absolute top-1/2 left-4 -translate-y-1/2"
              aria-hidden="true"
            />
            <input
              v-model="searchInput"
              type="text"
              placeholder="Search by post title (min. 2 characters)..."
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded-lg border py-3 pr-4 pl-12 focus:outline-none"
              @keyup.enter="handleSearch"
            />
            <button
              v-if="searchInput"
              type="button"
              class="text-ink-muted/70 hover:text-ink absolute top-1/2 right-4 -translate-y-1/2 transition-colors"
              aria-label="Clear search"
              @click="handleClearSearch"
            >
              <FontAwesomeIcon :icon="faXmark" aria-hidden="true" />
            </button>
          </div>
          <button
            type="button"
            class="bg-primary text-primary-contrast flex items-center gap-2 rounded-lg px-6 py-3 transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!isSearchValid"
            @click="handleSearch"
          >
            <FontAwesomeIcon :icon="faMagnifyingGlass" aria-hidden="true" />
            Search
          </button>
        </div>

        <!-- Tag filter -->
        <div class="flex flex-wrap items-center gap-2">
          <TagFilterPopover
            v-model="appliedTags"
            :all-values="allTags"
            label="Filter by Tags"
            panel-title="Select Tags"
            search-placeholder="Search tags..."
          />
        </div>

        <p v-if="searchQuery" class="font-kalam text-ink-muted text-sm">
          Found {{ filteredPosts.length }} post{{ filteredPosts.length !== 1 ? 's' : '' }}
        </p>
        <p v-else-if="appliedTags.length > 0" class="font-kalam text-ink-muted text-sm">
          Found {{ filteredPosts.length }} post{{ filteredPosts.length !== 1 ? 's' : '' }} with selected tags
        </p>
      </div>

      <!-- Grid -->
      <div v-if="pagedPosts.length > 0" class="grid grid-cols-1 items-start gap-8 md:grid-cols-2 lg:grid-cols-3">
        <BlogCard
          v-for="post in pagedPosts"
          :key="post.id"
          :ref="(el) => setCardRef(post.id, el)"
          :post="post"
          :style="cardMinHeight > 0 ? { minHeight: `${cardMinHeight}px` } : undefined"
        />
      </div>
      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="font-kalam text-ink-muted text-lg">
          {{ searchQuery || appliedTags.length > 0 ? 'No posts found matching your filters' : 'No posts yet' }}
        </p>
        <button
          v-if="searchQuery || appliedTags.length > 0"
          type="button"
          class="text-primary font-salsa mt-4 hover:opacity-80"
          @click="clearAllFilters"
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faChevronLeft, faChevronRight, faMagnifyingGlass, faXmark } from '@fortawesome/free-solid-svg-icons'
import { useCachedResource } from '@/composables/useCachedResource'
import { useUniformCardHeight } from '@/composables/useUniformCardHeight'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'
import BlogCard from './BlogCard.vue'
import TagFilterPopover from './TagFilterPopover.vue'
import type { BlogPost } from '@/types/blog'

// Cache key changed (was "blog-content") since the old mock's cached shape
// ({ posts: [...] }) doesn't match the real API's plain-array response - reusing the old
// key would read a stale, wrongly-shaped cache back as if it were the new format.
const { data: blogContent } = useCachedResource<BlogPost[]>('blog-content-v2', `${getApiBaseUrl()}/api/blog`)

const ITEMS_PER_PAGE = 12
const MIN_SEARCH_LENGTH = 2
const MAX_SEARCH_LENGTH = 200
const HAS_ALPHANUMERIC_REGEX = /[a-zA-Z0-9]/

const page = ref(1)
const searchInput = ref('')
const searchQuery = ref('')
const appliedTags = ref<string[]>([])

function parseTags(tags: string): string[] {
  return tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

function isValidSearchTerm(term: string): boolean {
  return term.length >= MIN_SEARCH_LENGTH && term.length <= MAX_SEARCH_LENGTH && HAS_ALPHANUMERIC_REGEX.test(term)
}

const isSearchValid = computed(() => {
  const trimmed = searchInput.value.trim()
  return trimmed.length === 0 || isValidSearchTerm(trimmed)
})

function handleSearch() {
  const trimmed = searchInput.value.trim()
  if (trimmed.length === 0) {
    searchQuery.value = ''
    return
  }
  if (!isValidSearchTerm(trimmed)) return
  searchQuery.value = trimmed
}

function handleClearSearch() {
  searchInput.value = ''
  searchQuery.value = ''
}

function clearAllFilters() {
  handleClearSearch()
  appliedTags.value = []
}

const publishedPosts = computed(() => {
  const posts = blogContent.value ?? []
  return [...posts].sort((a, b) => new Date(b.publishedAt ?? 0).getTime() - new Date(a.publishedAt ?? 0).getTime())
})

const allTags = computed(() => {
  const tagSet = new Set<string>()
  publishedPosts.value.forEach((post) => parseTags(post.tags).forEach((tag) => tagSet.add(tag)))
  return Array.from(tagSet).sort()
})

const filteredPosts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return publishedPosts.value.filter((post) => {
    const matchesSearch = query === '' || post.title.toLowerCase().includes(query)
    const postTags = parseTags(post.tags)
    const matchesTags = appliedTags.value.every((tag) => postTags.includes(tag))
    return matchesSearch && matchesTags
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPosts.value.length / ITEMS_PER_PAGE)))

const pagedPosts = computed(() => {
  const start = (page.value - 1) * ITEMS_PER_PAGE
  return filteredPosts.value.slice(start, start + ITEMS_PER_PAGE)
})

watch([searchQuery, appliedTags], () => {
  page.value = 1
})

const { cardMinHeight, setCardRef } = useUniformCardHeight(() => pagedPosts.value)
</script>
