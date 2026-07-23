// Mirrors the UI's src/types/blog.ts - keep in sync.
export interface BlogPost {
  id: string
  slug: string
  title: string
  excerpt: string
  content: string // Markdown.
  coverImage?: string
  published: boolean
  publishedAt?: string
  tags: string // Comma-separated.
  // Not edited directly here - the public UI layers its own vote on top (see useBlogReactions).
  likeCount: number
  dislikeCount: number
}
