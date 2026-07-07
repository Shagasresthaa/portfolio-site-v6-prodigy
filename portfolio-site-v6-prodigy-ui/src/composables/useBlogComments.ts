import { ref } from 'vue'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'
import type { BlogComment } from '@/types/blog'

export function useBlogComments(slug: string) {
  const comments = ref<BlogComment[]>([])
  const loading = ref(true)
  const error = ref<string | null>(null)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const response = await fetch(`${getApiBaseUrl()}/api/blog/${slug}/comments`)
      if (!response.ok) throw new Error(`Failed to fetch comments: ${response.status}`)
      comments.value = (await response.json()) as BlogComment[]
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load comments.'
    } finally {
      loading.value = false
    }
  }

  void refresh()

  async function addComment(name: string | undefined, content: string) {
    const response = await fetch(`${getApiBaseUrl()}/api/blog/${slug}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, content }),
    })
    if (!response.ok) throw new Error('Failed to post comment.')
    const comment = (await response.json()) as BlogComment
    comments.value = [comment, ...comments.value]
  }

  return { comments, loading, error, addComment }
}
