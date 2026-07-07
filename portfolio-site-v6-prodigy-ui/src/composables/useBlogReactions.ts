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
 * `vote` (this browser's own reaction) is tracked locally in localStorage purely to decide
 * which button renders highlighted - there's no visitor identity on the public site by design
 * (see memory: no-user-accounts-by-design), so the server can't look this up itself.
 *
 * `likeCount`/`dislikeCount` are NOT computed by adding the local vote on top of a base count -
 * that would double-count once the base count itself already reflects a persisted vote (e.g.
 * after `getBaseLikeCount`'s cache TTL expires and refetches a total that already includes this
 * browser's earlier vote). Instead, the counts the server returns after each reaction call become
 * the authoritative override; `getBaseLikeCount`/`getBaseDislikeCount` (the post's fetched
 * counts) are only the fallback shown before this browser has voted this session.
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
      // Best-effort - the vote still toggles locally even if this fails; worst case the
      // displayed count is stale until the next full page reload.
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
