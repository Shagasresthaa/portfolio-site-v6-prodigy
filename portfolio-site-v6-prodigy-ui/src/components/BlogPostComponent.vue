<template>
  <div class="container mx-auto px-4 py-12">
    <RouterLink
      to="/blog"
      class="font-salsa bg-ink/5 hover:bg-ink/10 text-ink mb-6 inline-flex items-center gap-2 rounded-lg px-4 py-2 backdrop-blur-md transition-colors"
    >
      <FontAwesomeIcon :icon="faArrowLeft" aria-hidden="true" />
      <span>Back to Blog</span>
    </RouterLink>

    <template v-if="blogContent">
      <article
        v-if="post"
        class="border-ink-muted/20 bg-surface-muted/75 mx-auto max-w-4xl rounded-2xl border p-8 shadow-xl backdrop-blur-md"
      >
        <div v-if="post.coverImage" class="bg-ink/10 -mx-8 -mt-8 mb-8 overflow-hidden rounded-t-2xl">
          <img :src="post.coverImage" :alt="post.title" class="h-96 w-full object-cover" />
        </div>

        <header class="mb-8">
          <h1 class="font-salsa text-primary mb-4 text-4xl font-bold md:text-5xl">{{ post.title }}</h1>

          <div class="font-kalam text-ink-muted flex flex-wrap items-center gap-4">
            <div v-if="formattedDate" class="flex items-center gap-2">
              <FontAwesomeIcon :icon="faCalendar" aria-hidden="true" />
              <span>{{ formattedDate }}</span>
            </div>
            <div v-if="tags.length > 0" class="flex items-center gap-2">
              <FontAwesomeIcon :icon="faTags" aria-hidden="true" />
              <div class="flex flex-wrap gap-2">
                <span v-for="tag in tags" :key="tag" class="bg-ink/10 text-ink-muted rounded-full px-3 py-1 text-xs">
                  {{ tag }}
                </span>
              </div>
            </div>
          </div>
        </header>

        <!-- prose-pre/prose-code overrides hand styling off to the pre/code blocks
        since renderMarkdown already outputs fully-styled highlight.js markup -->
        <div
          class="prose dark:prose-invert prose-pre:m-0 prose-pre:bg-transparent prose-pre:p-0 prose-code:before:content-none prose-code:after:content-none font-kalam mb-12 max-w-none"
          v-html="renderedContent"
        />

        <!-- Reactions + Share -->
        <div class="border-ink-muted/20 font-salsa mb-12 flex flex-wrap items-center gap-3 border-t pt-8">
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition-colors"
            :class="vote === 'like' ? 'bg-primary text-primary-contrast' : 'bg-ink/5 text-ink-muted hover:bg-ink/10'"
            @click="toggleLike"
          >
            <FontAwesomeIcon :icon="faThumbsUp" aria-hidden="true" />
            {{ likeCount }}
          </button>
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition-colors"
            :class="vote === 'dislike' ? 'bg-danger text-surface' : 'bg-ink/5 text-ink-muted hover:bg-ink/10'"
            @click="toggleDislike"
          >
            <FontAwesomeIcon :icon="faThumbsDown" aria-hidden="true" />
            {{ dislikeCount }}
          </button>
          <button
            type="button"
            class="bg-ink/5 hover:bg-ink/10 text-ink-muted ml-auto flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition-colors"
            @click="handleShare"
          >
            <FontAwesomeIcon :icon="justCopied ? faCheck : faShareNodes" aria-hidden="true" />
            {{ justCopied ? 'Copied!' : 'Share' }}
          </button>
        </div>

        <!-- Comments -->
        <section>
          <h2 class="font-salsa text-primary mb-6 text-3xl font-bold">Comments ({{ comments.length }})</h2>

          <form
            class="border-ink-muted/20 bg-surface font-salsa mb-8 flex flex-col gap-4 rounded-2xl border p-6 backdrop-blur-md"
            @submit.prevent="handleCommentSubmit"
          >
            <label class="flex items-center gap-2 text-sm">
              <input v-model="postAnonymously" type="checkbox" class="accent-(--color-primary) h-4 w-4 rounded" />
              Post anonymously
            </label>

            <input
              v-if="!postAnonymously"
              v-model="commentName"
              type="text"
              maxlength="100"
              placeholder="Your name (optional)"
              class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) w-full rounded border p-3 text-sm placeholder:text-ink-muted/70 focus:outline-none"
            />

            <textarea
              v-model="commentContent"
              required
              rows="4"
              maxlength="2000"
              placeholder="Write your comment..."
              class="border-ink-muted/30 bg-surface-muted focus:border-(--color-primary) w-full rounded border p-3 text-sm placeholder:text-ink-muted/70 focus:outline-none"
            />

            <button
              type="submit"
              class="bg-secondary text-secondary-contrast self-start rounded-lg px-6 py-2 text-sm transition hover:opacity-90"
            >
              Post Comment
            </button>
          </form>

          <div class="space-y-4">
            <p v-if="comments.length === 0" class="font-kalam text-ink-muted text-center">
              No comments yet. Be the first to comment!
            </p>
            <div
              v-for="comment in comments"
              :key="comment.id"
              class="border-ink-muted/20 bg-surface rounded-2xl border p-6"
            >
              <div class="font-salsa mb-2 flex items-center gap-3">
                <span class="font-semibold">{{ comment.name || 'Anonymous' }}</span>
                <span class="text-ink-muted text-sm">{{ formatCommentDate(comment.createdAt) }}</span>
              </div>
              <p class="font-kalam">{{ comment.content }}</p>
            </div>
          </div>
        </section>
      </article>

      <p v-else class="text-ink-muted text-center">Post not found.</p>
    </template>

    <p v-else class="text-ink-muted text-center">Loading...</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faArrowLeft,
  faCalendar,
  faCheck,
  faShareNodes,
  faTags,
  faThumbsDown,
  faThumbsUp,
} from '@fortawesome/free-solid-svg-icons'
import { useCachedResource } from '@/composables/useCachedResource'
import { useBlogReactions } from '@/composables/useBlogReactions'
import { useBlogComments } from '@/composables/useBlogComments'
import { useDocumentMeta, type DocumentMeta } from '@/composables/useDocumentMeta'
import { useShare } from '@/composables/useShare'
import { renderMarkdown } from '@/utils/markdown'
import type { BlogPost } from '@/types/blog'

interface BlogContent {
  posts: BlogPost[]
}

const route = useRoute()
const { data: blogContent } = useCachedResource<BlogContent>('blog-content', '/data/blog.json')

const post = computed<BlogPost | null>(() => {
  const slug = route.params.slug
  const found = blogContent.value?.posts.find((p) => p.slug === slug)
  return found && found.published ? found : null
})

const tags = computed(() =>
  (post.value?.tags ?? '')
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean),
)

const formattedDate = computed(() => {
  if (!post.value?.publishedAt) return ''
  return new Date(post.value.publishedAt).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
})

const renderedContent = computed(() => (post.value ? renderMarkdown(post.value.content) : ''))

const documentMeta = computed<DocumentMeta | null>(() => {
  if (!post.value) return null
  return {
    title: post.value.title,
    description: post.value.excerpt,
    image: post.value.coverImage,
    url: `${window.location.origin}/blog/${post.value.slug}`,
  }
})
useDocumentMeta(documentMeta)

// Safe to read once (not reactively) - BlogPostView keys this component on
// the slug param, so a different post is a full remount, not a param update.
const slug = typeof route.params.slug === 'string' ? route.params.slug : ''

const { vote, likeCount, dislikeCount, toggleLike, toggleDislike } = useBlogReactions(
  slug,
  () => post.value?.likeCount ?? 0,
  () => post.value?.dislikeCount ?? 0,
)

const { comments, addComment } = useBlogComments(slug)

const { share, justCopied } = useShare()

function handleShare() {
  if (!post.value) return
  const url = `${window.location.origin}/blog/${post.value.slug}`
  void share({ title: post.value.title, text: post.value.excerpt, url })
}

const postAnonymously = ref(false)
const commentName = ref('')
const commentContent = ref('')

function handleCommentSubmit() {
  const content = commentContent.value.trim()
  if (!content) return
  addComment(postAnonymously.value ? undefined : commentName.value.trim() || undefined, content)
  commentName.value = ''
  commentContent.value = ''
  postAnonymously.value = false
}

function formatCommentDate(iso: string): string {
  return new Date(iso).toLocaleDateString()
}
</script>

<style scoped>
/* Syntax highlighting for renderMarkdown's <pre><code class="hljs ..."> output
   (:deep needed since v-html content isn't visible to Vue's scoped CSS) -
   mapped onto our own design tokens rather than a stock highlight.js theme,
   so code blocks stay consistent with the rest of the app in both themes. */
.prose :deep(.hljs) {
  display: block;
  overflow-x: auto;
  border-radius: 0.5rem;
  padding: 1rem;
  background-color: var(--color-surface-muted);
  color: var(--color-ink);
}

.prose :deep(.hljs-comment),
.prose :deep(.hljs-quote) {
  color: var(--color-ink-muted);
  font-style: italic;
}

.prose :deep(.hljs-keyword),
.prose :deep(.hljs-selector-tag),
.prose :deep(.hljs-literal),
.prose :deep(.hljs-type) {
  color: var(--color-primary);
}

.prose :deep(.hljs-string),
.prose :deep(.hljs-number),
.prose :deep(.hljs-attr),
.prose :deep(.hljs-symbol) {
  color: var(--color-warning);
}

.prose :deep(.hljs-title),
.prose :deep(.hljs-function),
.prose :deep(.hljs-name) {
  color: var(--color-ink);
  font-weight: 600;
}
</style>
