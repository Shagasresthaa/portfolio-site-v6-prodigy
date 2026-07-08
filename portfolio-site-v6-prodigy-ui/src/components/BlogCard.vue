<template>
  <div
    class="border-ink-muted/20 bg-surface-muted/75 hover:bg-surface-muted flex flex-col overflow-hidden rounded-2xl border shadow-xl backdrop-blur-md transition-colors duration-300"
  >
    <RouterLink :to="`/blog/${post.slug}`" class="group">
      <div v-if="post.coverImage" class="bg-ink/10 relative aspect-video w-full overflow-hidden">
        <img
          :src="post.coverImage"
          :alt="post.title"
          class="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </div>
    </RouterLink>

    <div class="flex flex-1 flex-col p-6">
      <div class="mb-3">
        <RouterLink :to="`/blog/${post.slug}`" class="hover:opacity-80">
          <h2 class="font-salsa text-primary text-xl font-bold">{{ post.title }}</h2>
        </RouterLink>
        <div class="font-kalam text-ink-muted mt-2 flex items-center gap-2 text-sm">
          <FontAwesomeIcon :icon="faCalendar" class="shrink-0" aria-hidden="true" />
          <span>{{ formattedDate }}</span>
        </div>
      </div>

      <div class="grow">
        <p class="font-kalam" :class="!isExpanded && isLongExcerpt ? 'line-clamp-3' : ''">
          {{ post.excerpt }}
        </p>
        <button
          v-if="isLongExcerpt"
          type="button"
          class="text-primary font-salsa mt-1 text-sm hover:opacity-80"
          @click="isExpanded = !isExpanded"
        >
          {{ isExpanded ? 'Read less' : 'Read more' }}
        </button>
      </div>

      <div class="mt-4 flex flex-wrap gap-2">
        <span v-for="tag in tags" :key="tag" class="font-salsa bg-ink/10 text-ink-muted rounded-full px-2 py-1 text-xs">
          {{ tag }}
        </span>
      </div>

      <div class="font-salsa mt-4 flex items-center justify-between gap-3 pt-4">
        <RouterLink
          :to="`/blog/${post.slug}`"
          class="bg-secondary text-secondary-contrast flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm transition hover:opacity-90"
        >
          Read Post
          <FontAwesomeIcon :icon="faArrowRight" class="size-3" aria-hidden="true" />
        </RouterLink>
        <button
          type="button"
          class="bg-ink/5 hover:bg-ink/10 text-ink-muted flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm transition-colors"
          @click="handleShare"
        >
          <FontAwesomeIcon :icon="justCopied ? faCheck : faShareNodes" class="size-3.5" aria-hidden="true" />
          {{ justCopied ? 'Copied!' : 'Share' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faArrowRight, faCalendar, faCheck, faShareNodes } from '@fortawesome/free-solid-svg-icons'
import { useShare } from '@/composables/useShare'
import type { BlogPost } from '@/types/blog'

const props = defineProps<{ post: BlogPost }>()

const { share, justCopied } = useShare()

const isExpanded = ref(false)
const isLongExcerpt = computed(() => props.post.excerpt.length > 150)

const tags = computed(() =>
  props.post.tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean),
)

const formattedDate = computed(() => {
  if (!props.post.publishedAt) return ''
  return new Date(props.post.publishedAt).toLocaleDateString('en-US', {
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
})

function handleShare() {
  const url = `${window.location.origin}/blog/${props.post.slug}`
  void share({ title: props.post.title, text: props.post.excerpt, url })
}
</script>
