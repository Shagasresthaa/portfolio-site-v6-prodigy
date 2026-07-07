import { authFetch } from '@/composables/useApi'
import type { BlogPost } from '@/types/blog'

// Published posts first (newest first), then drafts alphabetically - there's no
// createdAt/updatedAt field on BlogPost to sort drafts by.
function sortPosts(posts: BlogPost[]): BlogPost[] {
  return [...posts].sort((a, b) => {
    if (a.published !== b.published) return a.published ? -1 : 1
    if (a.published && b.published) {
      return new Date(b.publishedAt ?? 0).getTime() - new Date(a.publishedAt ?? 0).getTime()
    }
    return a.title.localeCompare(b.title)
  })
}

/** Admin sees every post, drafts included - the public site only ever sees published ones. */
export async function loadBlogPosts(): Promise<BlogPost[]> {
  const response = await authFetch('/api/admin/blog')
  if (!response.ok) throw new Error(`Failed to fetch blog posts: ${response.status}`)
  const posts = (await response.json()) as BlogPost[]
  return sortPosts(posts)
}

export async function createBlogPost(post: Omit<BlogPost, 'id' | 'publishedAt' | 'likeCount' | 'dislikeCount'>): Promise<BlogPost> {
  const response = await authFetch('/api/admin/blog', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(post),
  })
  if (!response.ok) throw new Error('Failed to create post.')
  return (await response.json()) as BlogPost
}

export async function updateBlogPost(
  id: string,
  post: Omit<BlogPost, 'id' | 'publishedAt' | 'likeCount' | 'dislikeCount'>,
): Promise<BlogPost> {
  const response = await authFetch(`/api/admin/blog/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(post),
  })
  if (!response.ok) throw new Error('Failed to update post.')
  return (await response.json()) as BlogPost
}

export async function deleteBlogPost(id: string): Promise<void> {
  const response = await authFetch(`/api/admin/blog/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error('Failed to delete post.')
}
