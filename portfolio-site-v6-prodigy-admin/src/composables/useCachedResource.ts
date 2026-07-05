import { ref } from 'vue'

const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000

interface CacheEntry<T> {
  data: T
  fetchedAt: number
}

function readCache<T>(key: string): CacheEntry<T> | null {
  const raw = localStorage.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw) as CacheEntry<T>
  } catch {
    return null
  }
}

function writeCache<T>(key: string, data: T) {
  const entry: CacheEntry<T> = { data, fetchedAt: Date.now() }
  localStorage.setItem(key, JSON.stringify(entry))
}

/**
 * Loads JSON from `url`, caching it in localStorage under `key` for `ttlMs`.
 * Swapping `url` from a static JSON file to a real API endpoint later requires
 * no other changes here - the caching/staleness logic stays the same. Ported
 * from portfolio-site-v6-prodigy-ui's composable of the same name - keep the
 * two in sync if this logic changes.
 */
export function useCachedResource<T>(key: string, url: string, ttlMs = ONE_WEEK_MS) {
  const data = ref<T | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load() {
    // Astro prerenders this island's initial markup on the server (even with
    // client:load), where localStorage doesn't exist - skip loading there
    // and let the client-side hydration pass (a fresh setup() run, this time
    // in the browser) do the real fetch/cache-read instead.
    if (typeof localStorage === 'undefined') return

    const cached = readCache<T>(key)
    const isFresh = cached !== null && Date.now() - cached.fetchedAt < ttlMs
    if (isFresh) {
      data.value = cached.data
      return
    }

    loading.value = true
    error.value = null
    try {
      const response = await fetch(url)
      if (!response.ok) throw new Error(`Failed to fetch ${url}: ${response.status}`)
      const fresh = (await response.json()) as T
      data.value = fresh
      writeCache(key, fresh)
    } catch (err) {
      if (cached) {
        // Fetch failed - fall back to stale cache rather than showing nothing.
        data.value = cached.data
      } else {
        error.value = err instanceof Error ? err.message : 'Failed to load content'
      }
    } finally {
      loading.value = false
    }
  }

  void load()

  return { data, loading, error }
}
