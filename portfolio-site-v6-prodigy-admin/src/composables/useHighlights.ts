import type { HighlightItem } from '@/types/highlight'

const OVERRIDES_KEY = 'admin-highlights-overrides'
const BASE_URL = '/data/highlights.json'

interface Overrides {
  // Both edits to existing items and brand-new items live here, keyed by id
  // - there's no separate "create" endpoint to call yet (see CLAUDE.md:
  // GalleryController is scaffolded but empty), so new items are just
  // upserts whose id doesn't match anything in the base JSON.
  upserts: Record<string, HighlightItem>
  deletedIds: string[]
}

function readOverrides(): Overrides {
  const raw = localStorage.getItem(OVERRIDES_KEY)
  if (!raw) return { upserts: {}, deletedIds: [] }
  try {
    const parsed = JSON.parse(raw) as Partial<Overrides>
    return { upserts: parsed.upserts ?? {}, deletedIds: parsed.deletedIds ?? [] }
  } catch {
    return { upserts: {}, deletedIds: [] }
  }
}

function writeOverrides(overrides: Overrides) {
  localStorage.setItem(OVERRIDES_KEY, JSON.stringify(overrides))
}

async function fetchBaseHighlights(): Promise<HighlightItem[]> {
  const response = await fetch(BASE_URL)
  if (!response.ok) throw new Error(`Failed to fetch ${BASE_URL}: ${response.status}`)
  const data = (await response.json()) as { items: HighlightItem[] }
  return data.items
}

/** Base items from highlights.json with local create/edit/delete overrides layered on top. */
export async function loadHighlights(): Promise<HighlightItem[]> {
  const base = await fetchBaseHighlights()
  const { upserts, deletedIds } = readOverrides()
  const deleted = new Set(deletedIds)

  const existing = base.filter((item) => !deleted.has(item.id)).map((item) => upserts[item.id] ?? item)
  const created = Object.values(upserts).filter(
    (item) => !base.some((baseItem) => baseItem.id === item.id) && !deleted.has(item.id),
  )

  return [...created, ...existing].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  )
}

/** Throws if the browser's storage quota is exceeded (large images add up fast). */
export function upsertHighlight(item: HighlightItem) {
  const overrides = readOverrides()
  overrides.upserts[item.id] = item
  writeOverrides(overrides)
}

export function deleteHighlight(id: string) {
  const overrides = readOverrides()
  delete overrides.upserts[id]
  overrides.deletedIds = [...new Set([...overrides.deletedIds, id])]
  writeOverrides(overrides)
}
