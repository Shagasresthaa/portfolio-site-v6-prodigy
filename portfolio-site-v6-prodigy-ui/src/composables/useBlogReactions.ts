import { computed, ref } from 'vue'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'

type Vote = 'like' | 'dislike' | null
type ApiVote = 'LIKE' | 'DISLIKE'

const STORAGE_PREFIX = 'blog-reaction-'

function readVote(slug: string): Vote {
  const raw = localStorage.getItem(STORAGE_PREFIX + slug)
  return raw === 'like' || raw === 'dislike' ? raw : null
}

function toApiVote(vote: Vote): ApiVote | null {
  if (vote === 'like') return 'LIKE'
  if (vote === 'dislike') return 'DISLIKE'
  return null
}

async function submitReaction(
  slug: string,
  previousVote: Vote,
  vote: Vote,
): Promise<{ likeCount: number; dislikeCount: number }> {
  const response = await fetch(`${getApiBaseUrl()}/api/blog/${slug}/reactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ previousVote: toApiVote(previousVote), vote: toApiVote(vote) }),
  })
  if (!response.ok) throw new Error(`Failed to submit reaction: ${response.status}`)
  return (await response.json()) as { likeCount: number; dislikeCount: number }
}

/**
 * `vote` is tracked in localStorage only to decide which button highlights - no visitor
 * identity exists on this site, so the server can't look up a browser's past vote itself.
 *
 * Counts are never computed by adding the local vote onto a base count (would double-count
 * once a refreshed base already includes it) - the server's response after each reaction call
 * is always the authoritative count; `getBaseLikeCount`/`getBaseDislikeCount` are only the
 * pre-vote fallback.
 */
export function useBlogReactions(slug: string, getBaseLikeCount: () => number, getBaseDislikeCount: () => number) {
  const vote = ref<Vote>(readVote(slug))
  const likeCountOverride = ref<number | null>(null)
  const dislikeCountOverride = ref<number | null>(null)

  const likeCount = computed(() => likeCountOverride.value ?? getBaseLikeCount())
  const dislikeCount = computed(() => dislikeCountOverride.value ?? getBaseDislikeCount())

  async function setVote(next: Exclude<Vote, null>) {
    const previousVote = vote.value
    const nextVote = previousVote === next ? null : next

    vote.value = nextVote
    if (nextVote === null) localStorage.removeItem(STORAGE_PREFIX + slug)
    else localStorage.setItem(STORAGE_PREFIX + slug, nextVote)

    try {
      const counts = await submitReaction(slug, previousVote, nextVote)
      likeCountOverride.value = counts.likeCount
      dislikeCountOverride.value = counts.dislikeCount
    } catch {
      // Best-effort - vote still toggles locally; count just stays stale until reload.
    }
  }

  return {
    vote,
    likeCount,
    dislikeCount,
    toggleLike: () => setVote('like'),
    toggleDislike: () => setVote('dislike'),
  }
}
