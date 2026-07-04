import { computed, ref } from 'vue'

type Vote = 'like' | 'dislike' | null

const STORAGE_PREFIX = 'blog-reaction-'

function readVote(slug: string): Vote {
  const raw = localStorage.getItem(STORAGE_PREFIX + slug)
  return raw === 'like' || raw === 'dislike' ? raw : null
}

/**
 * Like/dislike is mocked locally - there's no backend yet (see CLAUDE.md).
 * `getBaseLikeCount`/`getBaseDislikeCount` read from the post's sample data
 * (simulating other visitors); this browser's own vote is persisted in
 * localStorage and layered on top, toggling off if the same button is
 * clicked again and switching cleanly between like/dislike otherwise.
 *
 * These are getters, not plain numbers, because the post loads asynchronously
 * (useCachedResource) - the post is still null on the first synchronous
 * render, so a plain number captured at call time would permanently freeze
 * at 0 instead of picking up the real counts once the data arrives.
 */
export function useBlogReactions(slug: string, getBaseLikeCount: () => number, getBaseDislikeCount: () => number) {
  const vote = ref<Vote>(readVote(slug))

  const likeCount = computed(() => getBaseLikeCount() + (vote.value === 'like' ? 1 : 0))
  const dislikeCount = computed(() => getBaseDislikeCount() + (vote.value === 'dislike' ? 1 : 0))

  function setVote(next: Exclude<Vote, null>) {
    vote.value = vote.value === next ? null : next
    if (vote.value === null) localStorage.removeItem(STORAGE_PREFIX + slug)
    else localStorage.setItem(STORAGE_PREFIX + slug, vote.value)
  }

  return {
    vote,
    likeCount,
    dislikeCount,
    toggleLike: () => setVote('like'),
    toggleDislike: () => setVote('dislike'),
  }
}
