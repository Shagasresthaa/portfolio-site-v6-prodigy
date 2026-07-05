import type { TimelineEvent } from '@/types/timeline'

export interface HomeContent {
  aboutHook: string
  aboutStory: string[]
  timeline: TimelineEvent[]
}

const OVERRIDE_KEY = 'admin-home-content-override'
const BASE_URL = '/data/home.json'

/**
 * Edits here are mocked locally - there's no endpoint yet to actually push
 * changes to the public site's /api/home (see CLAUDE.md: the public UI's
 * HomeComponent reads this same shape from public/data/home.json via its own
 * useCachedResource). A saved override in localStorage wins over the
 * baseline home.json shipped alongside this admin app; resetting clears it.
 *
 * Deliberately plain functions rather than a reactive composable - the
 * caller does one straightforward `await loadHomeContent()` in onMounted
 * instead of wiring up cross-composable computed/watch chains.
 */
export function readHomeOverride(): HomeContent | null {
  const raw = localStorage.getItem(OVERRIDE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as HomeContent
  } catch {
    return null
  }
}

export function saveHomeOverride(content: HomeContent) {
  localStorage.setItem(OVERRIDE_KEY, JSON.stringify(content))
}

export function clearHomeOverride() {
  localStorage.removeItem(OVERRIDE_KEY)
}

export async function fetchBaseHomeContent(): Promise<HomeContent> {
  const response = await fetch(BASE_URL)
  if (!response.ok) throw new Error(`Failed to fetch ${BASE_URL}: ${response.status}`)
  return (await response.json()) as HomeContent
}

/** Override if one's been saved, otherwise the baseline JSON. */
export async function loadHomeContent(): Promise<{ content: HomeContent; isDraft: boolean }> {
  const override = readHomeOverride()
  if (override) return { content: override, isDraft: true }
  return { content: await fetchBaseHomeContent(), isDraft: false }
}
