export interface BlogPost {
  id: string
  slug: string
  title: string
  excerpt: string
  // Markdown.
  content: string
  coverImage?: string
  published: boolean
  publishedAt?: string
  // Comma-separated, matching the old site's storage format.
  tags: string
  // Baseline counts simulating other visitors - the current browser's own vote
  // (tracked locally, see useBlogReactions) is layered on top of these.
  likeCount: number
  dislikeCount: number
}

export interface BlogComment {
  id: string
  name?: string
  content: string
  createdAt: string
}
