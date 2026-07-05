import type { BlogPost } from '@/types/blog'

const OVERRIDES_KEY = 'admin-blog-overrides'
const BASE_URL = '/data/blog.json'

interface Overrides {
  // Both edits to existing posts and brand-new ones live here, keyed by id -
  // there's no separate "create" endpoint to call yet (see CLAUDE.md:
  // BlogController is scaffolded but empty), so new posts are just upserts
  // whose id doesn't match anything in the base JSON.
  upserts: Record<string, BlogPost>
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

async function fetchBasePosts(): Promise<BlogPost[]> {
  const response = await fetch(BASE_URL)
  if (!response.ok) throw new Error(`Failed to fetch ${BASE_URL}: ${response.status}`)
  const data = (await response.json()) as { posts: BlogPost[] }
  return data.posts
}

/** Base posts from blog.json with local create/edit/delete overrides layered on top. */
export async function loadBlogPosts(): Promise<BlogPost[]> {
  const base = await fetchBasePosts()
  const { upserts, deletedIds } = readOverrides()
  const deleted = new Set(deletedIds)

  const existing = base.filter((post) => !deleted.has(post.id)).map((post) => upserts[post.id] ?? post)
  const created = Object.values(upserts).filter(
    (post) => !base.some((basePost) => basePost.id === post.id) && !deleted.has(post.id),
  )

  // Published posts first (newest first), then drafts alphabetically -
  // there's no createdAt/updatedAt field on BlogPost to sort drafts by.
  return [...created, ...existing].sort((a, b) => {
    if (a.published !== b.published) return a.published ? -1 : 1
    if (a.published && b.published) {
      return new Date(b.publishedAt ?? 0).getTime() - new Date(a.publishedAt ?? 0).getTime()
    }
    return a.title.localeCompare(b.title)
  })
}

/** Throws if the browser's storage quota is exceeded (large cover images add up fast). */
export function upsertBlogPost(post: BlogPost) {
  const overrides = readOverrides()
  overrides.upserts[post.id] = post
  writeOverrides(overrides)
}

export function deleteBlogPost(id: string) {
  const overrides = readOverrides()
  delete overrides.upserts[id]
  overrides.deletedIds = [...new Set([...overrides.deletedIds, id])]
  writeOverrides(overrides)
}
