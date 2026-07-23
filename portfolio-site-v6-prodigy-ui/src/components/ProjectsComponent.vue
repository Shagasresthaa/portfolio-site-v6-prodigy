<template>
  <div class="container mx-auto px-4 py-12">
    <div class="mb-12 text-center">
      <h1 class="font-salsa text-primary mb-3 text-4xl font-bold md:text-5xl">Projects</h1>
      <p class="font-kalam text-ink-muted text-lg md:text-xl">
        "Simplicity is the ultimate sophistication" - Leonardo da Vinci
      </p>
    </div>

    <template v-if="projectsContent">
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
              placeholder="Search by project name (min. 2 characters)..."
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

        <!-- Tech stack filter -->
        <div class="flex flex-wrap items-center gap-2">
          <TagFilterPopover
            v-model="appliedTechStacks"
            :all-values="allTechStacks"
            label="Filter by Tech Stack"
            panel-title="Select Tech Stacks"
            search-placeholder="Search tech stacks..."
          />
        </div>

        <p v-if="searchQuery" class="font-kalam text-ink-muted text-sm">
          Found {{ filteredProjects.length }} project{{ filteredProjects.length !== 1 ? 's' : '' }}
        </p>
        <p v-else-if="appliedTechStacks.length > 0" class="font-kalam text-ink-muted text-sm">
          Found {{ filteredProjects.length }} project{{ filteredProjects.length !== 1 ? 's' : '' }} with selected tech
          stacks
        </p>
      </div>

      <!-- Grid -->
      <div v-if="pagedProjects.length > 0" class="grid grid-cols-1 items-start gap-8 md:grid-cols-2 lg:grid-cols-3">
        <ProjectCard
          v-for="project in pagedProjects"
          :key="project.id"
          :ref="(el) => setCardRef(project.id, el)"
          :project="project"
          :style="cardMinHeight > 0 ? { minHeight: `${cardMinHeight}px` } : undefined"
        />
      </div>
      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="font-kalam text-ink-muted text-lg">
          {{
            searchQuery || appliedTechStacks.length > 0
              ? 'No projects found matching your filters'
              : 'No projects to display yet'
          }}
        </p>
        <button
          v-if="searchQuery || appliedTechStacks.length > 0"
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
import ProjectCard from './ProjectCard.vue'
import TagFilterPopover from './TagFilterPopover.vue'
import type { Project } from '@/types/project'

// -v2: old mock's `{ projects: [...] }` cache shape doesn't match the API's plain array.
const { data: projectsContent } = useCachedResource<Project[]>(
  'projects-content-v2',
  `${getApiBaseUrl()}/api/projects`,
)

const ITEMS_PER_PAGE = 12
const MIN_SEARCH_LENGTH = 2
const MAX_SEARCH_LENGTH = 200
const HAS_ALPHANUMERIC_REGEX = /[a-zA-Z0-9]/

const page = ref(1)
const searchInput = ref('')
const searchQuery = ref('')
const appliedTechStacks = ref<string[]>([])

function parseTechStacks(techStacks: string): string[] {
  return techStacks
    .split(',')
    .map((tech) => tech.trim())
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
  appliedTechStacks.value = []
}

const sortedProjects = computed(() => {
  const projects = projectsContent.value ?? []
  return [...projects].sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime())
})

const allTechStacks = computed(() => {
  const stackSet = new Set<string>()
  sortedProjects.value.forEach((project) => parseTechStacks(project.techStacks).forEach((tech) => stackSet.add(tech)))
  return Array.from(stackSet).sort()
})

const filteredProjects = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return sortedProjects.value.filter((project) => {
    const matchesSearch = query === '' || project.name.toLowerCase().includes(query)
    const projectStacks = parseTechStacks(project.techStacks)
    const matchesTechStacks = appliedTechStacks.value.every((stack) => projectStacks.includes(stack))
    return matchesSearch && matchesTechStacks
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProjects.value.length / ITEMS_PER_PAGE)))

const pagedProjects = computed(() => {
  const start = (page.value - 1) * ITEMS_PER_PAGE
  return filteredProjects.value.slice(start, start + ITEMS_PER_PAGE)
})

watch([searchQuery, appliedTechStacks], () => {
  page.value = 1
})

const { cardMinHeight, setCardRef } = useUniformCardHeight(() => pagedProjects.value)
</script>
