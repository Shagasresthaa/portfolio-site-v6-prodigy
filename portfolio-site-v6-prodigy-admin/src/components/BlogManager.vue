<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-5xl px-4 py-16 font-salsa">
      <!-- List view -->
      <template v-if="view === 'list'">
        <div class="mb-8 flex items-center justify-between">
          <h1 class="text-3xl">Blog</h1>
          <div class="flex items-center gap-3">
            <a
              href="/blog/images"
              class="border-ink-muted/30 hover:bg-surface flex items-center gap-2 rounded-lg border px-4 py-2 transition"
            >
              <PhotoIcon class="size-4" aria-hidden="true" />
              Manage Images
            </a>
            <button
              type="button"
              class="bg-primary text-primary-contrast flex items-center gap-2 rounded-lg px-4 py-2 transition hover:opacity-90"
              @click="openNewForm"
            >
              <PlusIcon class="size-4" aria-hidden="true" />
              New Post
            </button>
          </div>
        </div>

        <p v-if="loading" class="text-ink-muted text-center">Loading…</p>
        <p v-else-if="loadError" class="text-danger text-center">{{ loadError }}</p>

        <div v-else-if="posts.length > 0" class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="post in posts"
            :key="post.id"
            class="border-ink-muted/20 bg-surface-muted/70 overflow-hidden rounded-2xl border"
          >
            <div class="bg-surface h-40 w-full">
              <img
                v-if="post.coverImage"
                :src="post.coverImage"
                :alt="post.title"
                class="h-full w-full object-cover"
              />
              <div v-else class="flex h-full items-center justify-center">
                <DocumentTextIcon class="text-ink-muted size-10" aria-hidden="true" />
              </div>
            </div>

            <div class="p-4">
              <div class="mb-2 flex items-start justify-between gap-2">
                <h2 class="text-lg font-semibold">{{ post.title }}</h2>
                <span
                  :class="
                    post.published ? 'bg-secondary/20 text-secondary' : 'bg-warning/20 text-warning'
                  "
                  class="flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-xs whitespace-nowrap"
                >
                  <EyeIcon v-if="post.published" class="size-3" aria-hidden="true" />
                  <EyeSlashIcon v-else class="size-3" aria-hidden="true" />
                  {{ post.published ? 'Published' : 'Draft' }}
                </span>
              </div>

              <p class="text-ink-muted mb-1 text-xs">/{{ post.slug }}</p>
              <p class="text-ink-muted mb-2 line-clamp-2 text-sm">{{ post.excerpt }}</p>

              <div v-if="post.tags" class="mb-3 flex flex-wrap gap-1">
                <span
                  v-for="tag in post.tags.split(',').map((t) => t.trim())"
                  :key="tag"
                  class="bg-surface text-ink-muted rounded-full px-2 py-0.5 text-xs"
                >
                  {{ tag }}
                </span>
              </div>

              <div class="flex gap-2">
                <button
                  type="button"
                  class="bg-warning-soft text-warning flex flex-1 items-center justify-center gap-1 rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="openEditForm(post)"
                >
                  <PencilIcon class="size-4" aria-hidden="true" />
                  Edit
                </button>
                <button
                  type="button"
                  class="bg-danger-soft text-danger rounded-lg px-3 py-2 text-sm transition hover:opacity-80"
                  @click="handleDelete(post)"
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
          <p class="text-ink-muted text-xl">No posts yet</p>
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
          Back to posts
        </button>

        <h1 class="mb-6 text-3xl">{{ editingId ? 'Edit Post' : 'New Post' }}</h1>

        <form
          class="border-ink-muted/20 bg-surface-muted/70 flex flex-col gap-6 rounded-2xl border p-8 shadow-xl"
          @submit.prevent="handleSave"
        >
          <div class="grid gap-6 sm:grid-cols-2">
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
              <label class="mb-2 block text-sm font-semibold" for="slug">Slug *</label>
              <div class="flex gap-2">
                <input
                  id="slug"
                  v-model="form.slug"
                  type="text"
                  required
                  class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
                />
                <button
                  type="button"
                  class="border-ink-muted/30 hover:bg-surface rounded-lg border px-4 text-sm whitespace-nowrap transition"
                  @click="generateSlug"
                >
                  Generate
                </button>
              </div>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="coverImage">Cover image</label>
            <input
              id="coverImage"
              type="file"
              accept="image/*"
              class="border-ink-muted/30 bg-surface w-full rounded border p-3 text-sm"
              @change="handleCoverFileChange"
            />
            <p class="text-ink-muted mt-1 text-xs">Converted to WebP automatically.</p>
            <p v-if="processingImage" class="text-ink-muted mt-2 text-sm">Processing image…</p>
            <p v-if="imageError" class="text-danger mt-2 text-sm">{{ imageError }}</p>
            <img
              v-if="coverPreview"
              :src="coverPreview"
              alt="Cover preview"
              class="border-ink-muted/30 mt-4 h-32 w-auto rounded border object-cover"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="excerpt">Excerpt *</label>
            <textarea
              id="excerpt"
              v-model="form.excerpt"
              required
              rows="3"
              placeholder="A short preview of your post…"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold">Content (Markdown) *</label>
            <BlogImagePicker class="mb-3" @select="handleInsertImage" />
            <MdEditor
              ref="editorRef"
              v-model="form.content"
              :theme="themeStore.theme"
              language="en-US"
              :toolbars-exclude="['github', 'mermaid', 'katex']"
              :no-katex="true"
              :no-mermaid="true"
              :no-echarts="true"
              :sanitize="sanitizeHtml"
              style="height: 32rem"
              @on-upload-img="handleUploadImg"
            />
          </div>

          <div>
            <label class="mb-2 block text-sm font-semibold" for="tags">Tags</label>
            <input
              id="tags"
              v-model="form.tags"
              type="text"
              placeholder="programming, typescript, vue"
              class="border-ink-muted/30 bg-surface focus:border-(--color-primary) w-full rounded border p-3 text-base focus:outline-none"
            />
            <p class="text-ink-muted mt-1 text-xs">Comma-separated.</p>
          </div>

          <p class="text-ink-muted text-xs">
            Link previews use the title, excerpt, and cover image above directly - no separate SEO
            fields needed per post. Site-wide SEO defaults (for pages without their own content,
            plus search indexing) live under <a href="/settings" class="text-primary hover:underline">Settings</a>.
          </p>

          <label
            class="border-ink-muted/20 bg-surface flex items-center gap-3 rounded-lg border p-4"
          >
            <input v-model="form.published" type="checkbox" class="accent-(--color-primary) h-5 w-5 rounded" />
            <span class="font-semibold">Publish this post</span>
          </label>

          <p v-if="saveError" class="text-danger text-sm">{{ saveError }}</p>

          <div class="flex gap-4">
            <button
              type="submit"
              :disabled="saving || processingImage"
              class="bg-primary text-primary-contrast rounded-lg px-8 py-3 transition hover:opacity-90 disabled:opacity-60"
            >
              {{ saving ? 'Saving…' : editingId ? 'Update Post' : 'Create Post' }}
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
  DocumentTextIcon,
  EyeIcon,
  EyeSlashIcon,
  PencilIcon,
  PhotoIcon,
  PlusIcon,
  TrashIcon,
} from '@heroicons/vue/24/outline'
import { MdEditor, type ExposeParam } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import DOMPurify from 'dompurify'
import AuthGate from '@/components/AuthGate.vue'
import BlogImagePicker from '@/components/BlogImagePicker.vue'
import { createBlogPost, deleteBlogPost, loadBlogPosts, updateBlogPost } from '@/composables/useBlogPosts'
import { processSingleImageUpload } from '@/composables/useImageProcessing'
import { uploadImage } from '@/composables/useImageUpload'
import { useThemeStore } from '@/stores/theme'
import type { BlogPost } from '@/types/blog'
import type { BlogImage } from '@/types/blogImage'

const themeStore = useThemeStore()
const editorRef = ref<ExposeParam | null>(null)

const view = ref<'list' | 'form'>('list')
const posts = ref<BlogPost[]>([])
const loading = ref(true)
const loadError = ref<string | null>(null)

const editingId = ref<string | null>(null)
const form = reactive({
  title: '',
  slug: '',
  excerpt: '',
  content: '',
  tags: '',
  published: false,
})

// Inserts at cursor; falls back to appending if the editor ref isn't ready.
function handleInsertImage(image: BlogImage) {
  const markdown = `![${image.altText ?? ''}](${image.url})`
  if (editorRef.value) {
    editorRef.value.insert(() => ({ targetValue: markdown }))
  } else {
    form.content += `\n${markdown}\n`
  }
}
// Rendered <img :src>: real uploaded URL, or a local object URL when pendingCoverBlob is set (uploads on save).
const coverPreview = ref<string | null>(null)
const pendingCoverBlob = ref<Blob | null>(null)
const processingImage = ref(false)
const imageError = ref<string | null>(null)
const saving = ref(false)
const saveError = ref<string | null>(null)

function revokePendingCoverObjectUrl() {
  if (pendingCoverBlob.value && coverPreview.value) {
    URL.revokeObjectURL(coverPreview.value)
  }
}

function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html)
}

// Editor's paste/drop-to-upload: same WebP pipeline as the cover image, so inline images are hosted URLs, not base64.
async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  try {
    const urls = await Promise.all(
      files.map(async (file) => uploadImage(await processSingleImageUpload(file), 'blog')),
    )
    callback(urls)
  } catch (err) {
    console.error('Failed to process uploaded image', err)
    callback([])
  }
}

async function refreshList() {
  loading.value = true
  loadError.value = null
  try {
    posts.value = await loadBlogPosts()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to load posts.'
  } finally {
    loading.value = false
  }
}

onMounted(refreshList)

function generateSlug() {
  form.slug = form.title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '')
}

function resetForm() {
  form.title = ''
  form.slug = ''
  form.excerpt = ''
  form.content = ''
  form.tags = ''
  form.published = false
  revokePendingCoverObjectUrl()
  coverPreview.value = null
  pendingCoverBlob.value = null
  imageError.value = null
  saveError.value = null
}

function openNewForm() {
  editingId.value = null
  resetForm()
  view.value = 'form'
}

function openEditForm(post: BlogPost) {
  editingId.value = post.id
  form.title = post.title
  form.slug = post.slug
  form.excerpt = post.excerpt
  form.content = post.content
  form.tags = post.tags
  form.published = post.published
  pendingCoverBlob.value = null
  coverPreview.value = post.coverImage ?? null
  imageError.value = null
  saveError.value = null
  view.value = 'form'
}

async function handleCoverFileChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  processingImage.value = true
  imageError.value = null
  try {
    const blob = await processSingleImageUpload(file)
    revokePendingCoverObjectUrl()
    pendingCoverBlob.value = blob
    coverPreview.value = URL.createObjectURL(blob)
  } catch (err) {
    imageError.value = err instanceof Error ? err.message : 'Failed to process image.'
  } finally {
    processingImage.value = false
  }
}

async function handleDelete(post: BlogPost) {
  if (!confirm(`Delete "${post.title}"?`)) return
  try {
    await deleteBlogPost(post.id)
    await refreshList()
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Failed to delete post.'
  }
}

async function handleSave() {
  saveError.value = null

  if (!form.title.trim() || !form.slug.trim() || !form.excerpt.trim() || !form.content.trim()) {
    saveError.value = 'Please fill in all required fields.'
    return
  }

  saving.value = true
  try {
    let coverImage: string | undefined

    if (pendingCoverBlob.value) {
      // New file chosen this session - upload, then swap the local preview for the real URL.
      coverImage = await uploadImage(pendingCoverBlob.value, 'blog')
      revokePendingCoverObjectUrl()
      pendingCoverBlob.value = null
      coverPreview.value = coverImage
    } else {
      // Cover untouched - reuse the URL already shown in the preview.
      coverImage = coverPreview.value ?? undefined
    }

    const payload = {
      slug: form.slug.trim(),
      title: form.title.trim(),
      excerpt: form.excerpt.trim(),
      content: form.content,
      coverImage,
      tags: form.tags.trim(),
      published: form.published,
    }

    if (editingId.value) {
      await updateBlogPost(editingId.value, payload)
    } else {
      await createBlogPost(payload)
    }

    await refreshList()
    view.value = 'list'
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : 'Failed to save post.'
  } finally {
    saving.value = false
  }
}
</script>
