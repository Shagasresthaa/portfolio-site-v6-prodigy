<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-5xl px-4 py-16 font-salsa">
      <!-- List view -->
      <template v-if="view === 'list'">
        <div class="mb-8 flex items-center justify-between">
          <h1 class="text-3xl">Projects</h1>
          <button
            type="button"
            class="bg-primary text-primary-contrast flex items-center gap-2 rounded-lg px-4 py-2 transition hover:opacity-90"
            @click="openNewForm"
          >
            <PlusIcon class="size-4" aria-hidden="true" />
            New Project
          </button>
        </div>

        <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
        <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>

        <div v-else-if="projects.length > 0" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="project in projects"
            :key="project.id"
            class="border-ink-muted/20 bg-surface-muted/70 overflow-hidden rounded-2xl border"
          >
            <div class="bg-surface relative h-48 w-full">
              <img
                v-if="project.mediaType !== 'VIDEO' && project.image"
                :src="project.image"
                :alt="project.name"
                class="h-full w-full object-cover"
              />
              <img
                v-else-if="project.mediaType === 'VIDEO' && videoThumbnailUrl(project)"
                :src="videoThumbnailUrl(project)!"
                :alt="project.name"
                class="h-full w-full object-cover"
                @error="handleThumbnailError(project.id)"
              />
              <div v-else class="flex h-full items-center justify-center">
                <FolderOpenIcon class="text-ink-muted size-10" aria-hidden="true" />
              </div>
              <PlayCircleIcon
                v-if="project.mediaType === 'VIDEO' && videoThumbnailUrl(project)"
                class="absolute inset-0 m-auto size-12 text-white drop-shadow-lg"
                aria-hidden="true"
              />
            </div>

            <div class="p-4">
              <div class="mb-2 flex items-start justify-between gap-2">
                <h2 class="text-lg font-semibold">{{ project.name }}</h2>
                <span
                  :class="STATUS_BADGE_CLASSES[project.statusFlag]"
                  class="shrink-0 rounded-full px-2 py-0.5 text-xs whitespace-nowrap"
                >
                  {{ STATUS_LABELS[project.statusFlag] }}
                </span>
              </div>

              <p class="text-ink-muted mb-2 line-clamp-2 text-sm">{{ project.shortDesc }}</p>

              <div v-if="project.techStacks" class="mb-3 flex flex-wrap gap-1">
                <span
                  v-for="tech in project.techStacks.split(',').map((t) => t.trim())"
                  :key="tech"
                  class="bg-surface text-ink-muted rounded-full px-2 py-0.5 text-xs"
                >
                  {{ tech }}
                </span>
              </div>

              <p class="text-ink-muted mb-3 text-xs">{{ project.affiliation }}</p>

              <div class="flex gap-2">
                <button
                  type="button"
                  class="bg-warning-soft text-warning flex flex-1 items-center justify-center gap-1 rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="openEditForm(project)"
                >
                  <PencilIcon class="size-4" aria-hidden="true" />
                  Edit
                </button>
                <button
                  type="button"
                  class="bg-danger-soft text-danger rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="handleDelete(project)"
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
          <p class="text-ink-muted text-xl">No projects yet</p>
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
          Back to projects
        </button>

        <h1 class="mb-6 text-3xl">{{ editingId ? 'Edit Project' : 'New Project' }}</h1>

        <form
          class="border-ink-muted/20 bg-surface-muted/70 flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
          @submit.prevent="handleSave"
        >
          <div>
            <label class="mb-2 block text-sm font-semibold" for="name">Project name *</label>
            <input
              id="name"
              v-model="form.name"
              type="text"
              required
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="mediaType">Showcase media *</label>
            <select
              id="mediaType"
              v-model="form.mediaType"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            >
              <option value="IMAGE">Image</option>
              <option value="VIDEO">Video (YouTube demo)</option>
            </select>
          </div>

          <div v-if="form.mediaType === 'IMAGE'">
            <label class="mb-2 block text-sm font-semibold" for="image">Project image</label>
            <input
              id="image"
              type="file"
              accept="image/*"
              class="border-ink-muted/30 bg-surface w-full rounded border p-3 text-sm"
              @change="handleFileChange"
            />
            <p class="text-ink-muted mt-1 text-xs">Converted to WebP automatically.</p>
            <p v-if="processingImage" class="text-ink-muted mt-2 text-sm">Processing image…</p>
            <p v-if="imageError" class="text-danger mt-2 text-sm">{{ imageError }}</p>
            <img
              v-if="imagePreviewUrl"
              :src="imagePreviewUrl"
              alt="Preview"
              class="border-ink-muted/30 mt-4 h-32 w-auto rounded border object-cover"
            />
          </div>

          <div v-else>
            <label class="mb-2 block text-sm font-semibold" for="videoUrl">Demo video URL *</label>
            <input
              id="videoUrl"
              v-model="form.videoUrl"
              type="url"
              placeholder="https://www.youtube.com/watch?v=..."
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="shortDesc">Short description *</label>
            <textarea
              id="shortDesc"
              v-model="form.shortDesc"
              required
              rows="3"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="longDesc">Long description</label>
            <textarea
              id="longDesc"
              v-model="form.longDesc"
              rows="6"
              placeholder="Optional - shown on the project's detail view"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="statusFlag">Status *</label>
            <select
              id="statusFlag"
              v-model="form.statusFlag"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              @change="handleStatusChange"
            >
              <option v-for="status in PROJECT_STATUSES" :key="status" :value="status">
                {{ STATUS_LABELS[status] }}
              </option>
            </select>
          </div>

          <div class="grid gap-6 sm:grid-cols-2">
            <div>
              <label class="mb-2 block text-sm font-semibold" for="startDate">Start date *</label>
              <input
                id="startDate"
                v-model="form.startDate"
                type="date"
                required
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
            </div>
            <div v-if="shouldShowEndDate">
              <label class="mb-2 block text-sm font-semibold" for="endDate">
                End date {{ isEndDateRequired ? '*' : '' }}
              </label>
              <input
                id="endDate"
                v-model="form.endDate"
                type="date"
                :required="isEndDateRequired"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
              <p class="text-ink-muted mt-1 text-xs">
                {{
                  form.statusFlag === 'IN_PROGRESS'
                    ? "Leave empty to show as 'Present'"
                    : form.statusFlag === 'MAINTAINED'
                      ? 'Leave empty for ongoing maintenance'
                      : 'Required for completed/archived projects'
                }}
              </p>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="collabMode"
              >Collaboration mode *</label
            >
            <select
              id="collabMode"
              v-model="form.collabMode"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            >
              <option value="SOLO">Solo</option>
              <option value="GROUP">Group</option>
            </select>
          </div>

          <div class="grid gap-6 sm:grid-cols-2">
            <div>
              <label class="mb-2 block text-sm font-semibold" for="affiliation">Affiliation *</label>
              <input
                id="affiliation"
                v-model="form.affiliation"
                type="text"
                required
                placeholder="e.g. Western Michigan University"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
            </div>
            <div>
              <label class="mb-2 block text-sm font-semibold" for="affiliationType"
                >Affiliation type *</label
              >
              <select
                id="affiliationType"
                v-model="form.affiliationType"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              >
                <option value="INDEPENDENT">Independent</option>
                <option value="UNIVERSITY">University</option>
                <option value="ORGANIZATION">Organization</option>
                <option value="CLUB">Club</option>
              </select>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="sourceCodeAvailability"
              >Source code availability *</label
            >
            <select
              id="sourceCodeAvailability"
              v-model="form.sourceCodeAvailability"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            >
              <option value="OPEN_SOURCE">Open source</option>
              <option value="CLOSED_SOURCE">Closed source</option>
              <option value="UNDER_NDA">Under NDA</option>
            </select>
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="techStacks">Tech stacks *</label>
            <input
              id="techStacks"
              v-model="form.techStacks"
              type="text"
              required
              placeholder="Vue, TypeScript, Spring Boot"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
            <p class="text-ink-muted mt-1 text-xs">Comma-separated.</p>
          </div>

          <div class="grid gap-6 sm:grid-cols-2">
            <div>
              <label class="mb-2 block text-sm font-semibold" for="projectUrl"
                >Project URL (GitHub, etc.)</label
              >
              <input
                id="projectUrl"
                v-model="form.projectUrl"
                type="url"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
            </div>
            <div>
              <label class="mb-2 block text-sm font-semibold" for="liveUrl">Live URL</label>
              <input
                id="liveUrl"
                v-model="form.liveUrl"
                type="url"
                class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
              />
            </div>
          </div>

          <p v-if="saveError" class="text-danger text-sm">{{ saveError }}</p>

          <div class="flex gap-4">
            <button
              type="submit"
              :disabled="saving || processingImage"
              class="bg-primary text-primary-contrast rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
            >
              {{ saving ? 'Saving…' : editingId ? 'Update Project' : 'Create Project' }}
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
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ArrowLeftIcon,
  FolderOpenIcon,
  PencilIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/vue/24/outline'
import { PlayCircleIcon } from '@heroicons/vue/24/solid'
import AuthGate from '@/components/AuthGate.vue'
import { createProject, deleteProject, loadProjects, updateProject } from '@/composables/useProjects'
import { processSingleImageUpload } from '@/composables/useImageProcessing'
import { uploadImage } from '@/composables/useImageUpload'
import type {
  AffiliationType,
  CollabMode,
  Project,
  ProjectMediaType,
  ProjectStatus,
  SourceCodeAvailability,
} from '@/types/project'
import { getYoutubeThumbnailUrl } from '@/utils/youtube'

const PROJECT_STATUSES: ProjectStatus[] = [
  'PLANNING',
  'IN_PROGRESS',
  'COMPLETED',
  'MAINTAINED',
  'ARCHIVED',
]

const STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING: 'Planning',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
  MAINTAINED: 'Maintained',
  ARCHIVED: 'Archived',
}

// Same semantic-token mapping as ProjectCard.vue in the UI project - keep in sync.
const STATUS_BADGE_CLASSES: Record<ProjectStatus, string> = {
  PLANNING: 'bg-ink/10 text-ink-muted',
  IN_PROGRESS: 'bg-warning/20 text-warning',
  COMPLETED: 'bg-secondary/20 text-secondary',
  MAINTAINED: 'bg-primary/20 text-primary',
  ARCHIVED: 'bg-danger/20 text-danger',
}

const view = ref<'list' | 'form'>('list')
const projects = ref<Project[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)
const failedThumbnails = ref<Record<string, boolean>>({})

function videoThumbnailUrl(project: Project): string | null {
  if (failedThumbnails.value[project.id] || !project.videoUrl) return null
  return getYoutubeThumbnailUrl(project.videoUrl)
}

function handleThumbnailError(id: string) {
  failedThumbnails.value = { ...failedThumbnails.value, [id]: true }
}

const editingId = ref<string | null>(null)
const form = reactive({
  name: '',
  shortDesc: '',
  longDesc: '',
  statusFlag: 'PLANNING' as ProjectStatus,
  startDate: '',
  endDate: '',
  collabMode: 'SOLO' as CollabMode,
  affiliation: '',
  affiliationType: 'INDEPENDENT' as AffiliationType,
  sourceCodeAvailability: 'OPEN_SOURCE' as SourceCodeAvailability,
  techStacks: '',
  projectUrl: '',
  liveUrl: '',
  mediaType: 'IMAGE' as ProjectMediaType,
  videoUrl: '',
})
// Rendered <img :src>: real uploaded URL, or a local object URL when pendingImageBlob is set (uploads on save).
const imagePreviewUrl = ref<string | null>(null)
const pendingImageBlob = ref<Blob | null>(null)
const processingImage = ref(false)
const imageError = ref<string | null>(null)
const saving = ref(false)
const saveError = ref<string | null>(null)

function revokePendingObjectUrl() {
  if (pendingImageBlob.value && imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value)
  }
}

const shouldShowEndDate = computed(() => form.statusFlag !== 'PLANNING')
const isEndDateRequired = computed(
  () => form.statusFlag === 'COMPLETED' || form.statusFlag === 'ARCHIVED',
)

function handleStatusChange() {
  if (form.statusFlag === 'PLANNING' || form.statusFlag === 'IN_PROGRESS') {
    form.endDate = ''
  }
}

async function refreshList() {
  loading.value = true
  loadError.value = null
  try {
    projects.value = await loadProjects()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load projects.'
  } finally {
    loading.value = false
  }
}

onMounted(refreshList)

function toDateInputValue(iso: string | undefined): string {
  return iso ? iso.slice(0, 10) : ''
}

function fromDateInputValue(value: string): string {
  return new Date(`${value}T00:00:00.000Z`).toISOString()
}

function resetForm() {
  form.name = ''
  form.shortDesc = ''
  form.longDesc = ''
  form.statusFlag = 'PLANNING'
  form.startDate = ''
  form.endDate = ''
  form.collabMode = 'SOLO'
  form.affiliation = ''
  form.affiliationType = 'INDEPENDENT'
  form.sourceCodeAvailability = 'OPEN_SOURCE'
  form.techStacks = ''
  form.projectUrl = ''
  form.liveUrl = ''
  form.mediaType = 'IMAGE'
  form.videoUrl = ''
  revokePendingObjectUrl()
  imagePreviewUrl.value = null
  pendingImageBlob.value = null
  imageError.value = null
  saveError.value = null
}

function openNewForm() {
  editingId.value = null
  resetForm()
  view.value = 'form'
}

function openEditForm(project: Project) {
  editingId.value = project.id
  form.name = project.name
  form.shortDesc = project.shortDesc
  form.longDesc = project.longDesc ?? ''
  form.statusFlag = project.statusFlag
  form.startDate = toDateInputValue(project.startDate)
  form.endDate = toDateInputValue(project.endDate)
  form.collabMode = project.collabMode
  form.affiliation = project.affiliation
  form.affiliationType = project.affiliationType
  form.sourceCodeAvailability = project.sourceCodeAvailability
  form.techStacks = project.techStacks
  form.projectUrl = project.projectUrl ?? ''
  form.liveUrl = project.liveUrl ?? ''
  form.mediaType = project.mediaType ?? 'IMAGE'
  form.videoUrl = project.videoUrl ?? ''
  pendingImageBlob.value = null
  imagePreviewUrl.value = project.image ?? null
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
    const blob = await processSingleImageUpload(file)
    revokePendingObjectUrl()
    pendingImageBlob.value = blob
    imagePreviewUrl.value = URL.createObjectURL(blob)
  } catch (err) {
    imageError.value = err instanceof Error ? err.message : 'Failed to process image.'
  } finally {
    processingImage.value = false
  }
}

async function handleDelete(project: Project) {
  if (!confirm(`Delete "${project.name}"?`)) return
  try {
    await deleteProject(project.id)
    await refreshList()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to delete project.'
  }
}

async function handleSave() {
  saveError.value = null

  if (!form.name.trim() || !form.shortDesc.trim() || !form.affiliation.trim() || !form.techStacks.trim()) {
    saveError.value = 'Please fill in all required fields.'
    return
  }
  if (!form.startDate) {
    saveError.value = 'Start date is required.'
    return
  }
  if (isEndDateRequired.value && !form.endDate) {
    saveError.value = 'End date is required for completed/archived projects.'
    return
  }
  if (form.mediaType === 'IMAGE' && !imagePreviewUrl.value) {
    saveError.value = 'Please upload a project image.'
    return
  }
  if (form.mediaType === 'VIDEO' && !form.videoUrl.trim()) {
    saveError.value = 'Demo video URL is required.'
    return
  }

  saving.value = true
  try {
    let image: string | undefined

    if (form.mediaType === 'IMAGE') {
      if (pendingImageBlob.value) {
        // New file chosen this session - upload, then swap the local preview for the real URL.
        image = await uploadImage(pendingImageBlob.value, 'projects')
        revokePendingObjectUrl()
        pendingImageBlob.value = null
        imagePreviewUrl.value = image
      } else {
        // Image untouched - reuse the URL already shown in the preview.
        image = imagePreviewUrl.value!
      }
    }

    const payload: Omit<Project, 'id'> = {
      name: form.name.trim(),
      shortDesc: form.shortDesc.trim(),
      longDesc: form.longDesc.trim() || undefined,
      statusFlag: form.statusFlag,
      startDate: fromDateInputValue(form.startDate),
      endDate: shouldShowEndDate.value && form.endDate ? fromDateInputValue(form.endDate) : undefined,
      collabMode: form.collabMode,
      affiliation: form.affiliation.trim(),
      affiliationType: form.affiliationType,
      sourceCodeAvailability: form.sourceCodeAvailability,
      techStacks: form.techStacks.trim(),
      projectUrl: form.projectUrl.trim() || undefined,
      liveUrl: form.liveUrl.trim() || undefined,
      mediaType: form.mediaType,
      ...(form.mediaType === 'IMAGE' ? { image } : { videoUrl: form.videoUrl.trim() }),
    }

    if (editingId.value) {
      await updateProject(editingId.value, payload)
    } else {
      await createProject(payload)
    }

    await refreshList()
    view.value = 'list'
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : 'Failed to save project.'
  } finally {
    saving.value = false
  }
}
</script>
