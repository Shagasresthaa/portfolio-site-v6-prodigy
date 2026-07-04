import { ref } from 'vue'
import type { BlogComment } from '@/types/blog'

const STORAGE_PREFIX = 'blog-comments-'

function readComments(slug: string): BlogComment[] {
  const raw = localStorage.getItem(STORAGE_PREFIX + slug)
  if (!raw) return []
  try {
    return JSON.parse(raw) as BlogComment[]
  } catch {
    return []
  }
}

/**
 * Comments are mocked locally - there's no backend yet (see CLAUDE.md).
 * Persisted per browser in localStorage, newest first, same ordering as the
 * old site.
 */
export function useBlogComments(slug: string) {
  const comments = ref<BlogComment[]>(readComments(slug))

  function addComment(name: string | undefined, content: string) {
    const comment: BlogComment = {
      id: crypto.randomUUID(),
      name: name || undefined,
      content,
      createdAt: new Date().toISOString(),
    }
    comments.value = [comment, ...comments.value]
    localStorage.setItem(STORAGE_PREFIX + slug, JSON.stringify(comments.value))
  }

  return { comments, addComment }
}
