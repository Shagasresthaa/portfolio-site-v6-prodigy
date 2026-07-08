<template>
  <div
    class="border-ink-muted/20 bg-surface-muted/75 hover:bg-surface-muted group flex flex-col overflow-hidden rounded-2xl border shadow-xl backdrop-blur-md transition-colors duration-300"
  >
    <div
      v-if="project.mediaType !== 'VIDEO' && project.image"
      class="bg-ink/10 relative h-60 w-full overflow-hidden"
    >
      <img
        :src="project.image"
        :alt="project.name"
        class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
      />
    </div>
    <div
      v-else-if="project.mediaType === 'VIDEO' && project.videoUrl"
      class="bg-ink/10 relative h-60 w-full overflow-hidden"
    >
      <iframe
        :src="`https://www.youtube.com/embed/${youTubeId}`"
        :title="project.name"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowfullscreen
        class="absolute inset-0 h-full w-full"
      />
    </div>

    <div class="flex flex-1 flex-col p-6">
      <div class="mb-4">
        <h2 class="font-salsa text-primary mb-2 text-2xl font-bold">{{ project.name }}</h2>
        <div class="flex flex-wrap gap-2">
          <span class="rounded-full px-3 py-1 text-xs" :class="statusBadgeClass">{{ statusLabel }}</span>
          <span class="bg-ink/10 text-ink-muted rounded-full px-3 py-1 text-xs">{{ collabModeLabel }}</span>
          <span class="rounded-full px-3 py-1 text-xs" :class="sourceCodeBadgeClass">{{ sourceCodeLabel }}</span>
        </div>
      </div>

      <div class="font-kalam text-ink-muted mb-4 space-y-2 text-sm">
        <div class="flex items-center gap-2">
          <FontAwesomeIcon :icon="faCalendar" class="shrink-0" aria-hidden="true" />
          <span>{{ dateRangeLabel }}</span>
        </div>
        <div v-if="durationLabel" class="flex items-center gap-2">
          <FontAwesomeIcon :icon="faClock" class="shrink-0" aria-hidden="true" />
          <span>{{ durationLabel }}</span>
        </div>
        <div class="flex items-start gap-2">
          <FontAwesomeIcon :icon="faBuilding" class="mt-0.5 shrink-0" aria-hidden="true" />
          <span>{{ project.affiliation }} ({{ affiliationTypeLabel }})</span>
        </div>
      </div>

      <div class="grow">
        <p class="font-kalam">{{ project.shortDesc }}</p>

        <template v-if="project.longDesc">
          <p v-if="isExpanded" class="font-kalam mt-3">{{ project.longDesc }}</p>
          <button
            type="button"
            class="text-primary font-salsa mt-2 text-sm hover:opacity-80"
            @click="isExpanded = !isExpanded"
          >
            {{ isExpanded ? 'Show less' : 'Read more' }}
          </button>
        </template>
      </div>

      <div class="mt-auto space-y-4 pt-4">
        <div>
          <p class="text-ink-muted font-salsa mb-2 text-sm font-semibold">Tech Stack:</p>
          <div class="flex flex-wrap gap-2">
            <span v-for="tech in techStacks" :key="tech" class="font-salsa bg-ink/10 text-ink-muted rounded-lg px-2 py-1 text-xs">
              {{ tech }}
            </span>
          </div>
        </div>

        <div class="font-salsa flex gap-3">
          <a
            v-if="project.sourceCodeAvailability === 'OPEN_SOURCE' && project.projectUrl"
            :href="project.projectUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="bg-ink/10 hover:bg-ink/20 text-ink flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition-colors"
          >
            <FontAwesomeIcon :icon="faGithub" aria-hidden="true" />
            Code
          </a>
          <div
            v-else-if="project.sourceCodeAvailability === 'UNDER_NDA'"
            class="bg-ink/5 text-ink-muted flex items-center gap-2 rounded-lg px-4 py-2 text-sm"
          >
            <FontAwesomeIcon :icon="faLock" aria-hidden="true" />
            NDA
          </div>

          <a
            v-if="project.liveUrl"
            :href="project.liveUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="bg-secondary text-secondary-contrast flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition hover:opacity-90"
          >
            <FontAwesomeIcon :icon="faArrowUpRightFromSquare" aria-hidden="true" />
            Live Demo
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faArrowUpRightFromSquare, faBuilding, faCalendar, faClock, faLock } from '@fortawesome/free-solid-svg-icons'
import { faGithub } from '@fortawesome/free-brands-svg-icons'
import type { AffiliationType, Project, ProjectStatus, SourceCodeAvailability } from '@/types/project'
import { getYoutubeEmbedId } from '@/utils/youtube'

const props = defineProps<{ project: Project }>()

const isExpanded = ref(false)
const youTubeId = computed(() => getYoutubeEmbedId(props.project.videoUrl))

const techStacks = computed(() =>
  props.project.techStacks
    .split(',')
    .map((tech) => tech.trim())
    .filter(Boolean),
)

const STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING: 'Planning',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  MAINTAINED: 'Maintained',
  ARCHIVED: 'Archived',
}

const STATUS_BADGE_CLASSES: Record<ProjectStatus, string> = {
  PLANNING: 'bg-ink/10 text-ink-muted',
  IN_PROGRESS: 'bg-warning/20 text-warning',
  COMPLETED: 'bg-secondary/20 text-secondary',
  MAINTAINED: 'bg-primary/20 text-primary',
  ARCHIVED: 'bg-danger/20 text-danger',
}

const statusLabel = computed(() => STATUS_LABELS[props.project.statusFlag])
const statusBadgeClass = computed(() => STATUS_BADGE_CLASSES[props.project.statusFlag])

const COLLAB_MODE_LABELS: Record<'SOLO' | 'GROUP', string> = { SOLO: 'Solo', GROUP: 'Group' }
const collabModeLabel = computed(() => COLLAB_MODE_LABELS[props.project.collabMode])

const AFFILIATION_TYPE_LABELS: Record<AffiliationType, string> = {
  INDEPENDENT: 'Independent',
  UNIVERSITY: 'University',
  ORGANIZATION: 'Organization',
  CLUB: 'Club',
}
const affiliationTypeLabel = computed(() => AFFILIATION_TYPE_LABELS[props.project.affiliationType])

const SOURCE_CODE_LABELS: Record<SourceCodeAvailability, string> = {
  OPEN_SOURCE: 'Open Source',
  CLOSED_SOURCE: 'Closed Source',
  UNDER_NDA: 'Under NDA',
}

const SOURCE_CODE_BADGE_CLASSES: Record<SourceCodeAvailability, string> = {
  OPEN_SOURCE: 'bg-secondary/20 text-secondary',
  CLOSED_SOURCE: 'bg-ink/10 text-ink-muted',
  UNDER_NDA: 'bg-warning/20 text-warning',
}

const sourceCodeLabel = computed(() => SOURCE_CODE_LABELS[props.project.sourceCodeAvailability])
const sourceCodeBadgeClass = computed(() => SOURCE_CODE_BADGE_CLASSES[props.project.sourceCodeAvailability])

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('en-US', { month: 'short', year: 'numeric', timeZone: 'UTC' })
}

const dateRangeLabel = computed(() => {
  const { startDate, endDate, statusFlag } = props.project
  const start = formatDate(startDate)

  if (statusFlag === 'PLANNING') return `Expected: ${start}`
  if (statusFlag === 'IN_PROGRESS' && !endDate) return `${start} - Present`
  if (endDate) return `${start} - ${formatDate(endDate)}`
  return start
})

const durationLabel = computed(() => {
  const { startDate, endDate, statusFlag } = props.project
  if (statusFlag === 'PLANNING') return null

  const start = new Date(startDate)
  const end = endDate ? new Date(endDate) : new Date()
  const months = (end.getUTCFullYear() - start.getUTCFullYear()) * 12 + (end.getUTCMonth() - start.getUTCMonth())

  if (months < 1) return '< 1 month'
  if (months === 1) return '1 month'
  if (months < 12) return `${months} months`

  const years = Math.floor(months / 12)
  const remainingMonths = months % 12
  if (remainingMonths === 0) return `${years} ${years === 1 ? 'year' : 'years'}`
  return `${years} ${years === 1 ? 'year' : 'years'} ${remainingMonths} ${remainingMonths === 1 ? 'month' : 'months'}`
})
</script>
