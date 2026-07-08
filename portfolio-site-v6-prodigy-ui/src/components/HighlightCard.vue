<template>
  <div
    class="border-ink-muted/20 bg-surface-muted/75 hover:bg-surface-muted flex flex-col rounded-2xl border shadow-xl backdrop-blur-md transition-colors duration-300"
  >
    <!-- Media: fixed aspect ratio so mixed image/video cards line up in the grid. -->
    <div
      class="bg-ink/10 relative aspect-video w-full overflow-hidden rounded-t-2xl"
      :class="item.mediaType === 'IMAGE' ? 'group cursor-pointer' : ''"
      @click="item.mediaType === 'IMAGE' && $emit('view')"
    >
      <template v-if="item.mediaType === 'IMAGE'">
        <img
          :src="item.thumbnailImage"
          :alt="item.title"
          class="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
        <!-- Desktop: view affordance only on hover. -->
        <div
          class="bg-ink/0 group-hover:bg-ink/40 absolute inset-0 hidden items-center justify-center transition-colors duration-300 md:flex"
        >
          <div
            class="bg-surface/90 text-ink flex items-center gap-2 rounded-full px-4 py-2 opacity-0 shadow-lg transition-opacity duration-300 group-hover:opacity-100"
          >
            <FontAwesomeIcon :icon="faEye" class="size-4" aria-hidden="true" />
            <span class="font-salsa text-sm font-medium">View</span>
          </div>
        </div>
        <!-- Mobile: no hover, so the affordance is always visible instead. -->
        <div class="absolute top-2 right-2 md:hidden">
          <div class="bg-surface/90 text-ink flex items-center gap-1.5 rounded-full px-3 py-1.5 shadow-lg">
            <FontAwesomeIcon :icon="faEye" class="size-3.5" aria-hidden="true" />
            <span class="font-salsa text-xs font-medium">View</span>
          </div>
        </div>
      </template>
      <iframe
        v-else-if="item.videoUrl"
        :src="`https://www.youtube.com/embed/${youTubeId}`"
        :title="item.title"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowfullscreen
        class="absolute inset-0 h-full w-full"
      />
    </div>

    <div class="flex flex-1 flex-col p-4">
      <div class="grow">
        <h3 class="font-salsa text-primary mb-2 text-xl font-bold">{{ item.title }}</h3>

        <div v-if="item.description" class="mb-3">
          <p class="font-kalam" :class="!isExpanded && isLongDescription ? 'line-clamp-3' : ''">
            {{ item.description }}
          </p>
          <button
            v-if="isLongDescription"
            type="button"
            class="text-primary font-salsa mt-1 text-sm hover:opacity-80"
            @click="isExpanded = !isExpanded"
          >
            {{ isExpanded ? 'Read less' : 'Read more' }}
          </button>
        </div>
      </div>

      <div class="mt-auto space-y-3 pt-3">
        <p v-if="item.caption" class="font-kalam text-ink-muted text-sm italic">"{{ item.caption }}"</p>

        <div v-if="tags.length > 0" class="flex flex-wrap gap-2">
          <span v-for="tag in tags" :key="tag" class="font-salsa bg-ink/10 text-ink-muted rounded-full px-2 py-1 text-xs">
            {{ tag }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faEye } from '@fortawesome/free-solid-svg-icons'
import type { HighlightItem } from '@/types/highlight'
import { getYoutubeEmbedId } from '@/utils/youtube'

const props = defineProps<{ item: HighlightItem }>()
defineEmits<{ view: [] }>()

const isExpanded = ref(false)
const isLongDescription = computed(() => (props.item.description?.length ?? 0) > 150)

const tags = computed(() =>
  props.item.tags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean),
)

const youTubeId = computed(() => getYoutubeEmbedId(props.item.videoUrl))
</script>
