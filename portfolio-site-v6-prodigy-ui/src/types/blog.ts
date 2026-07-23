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
  // Server-authoritative; see useBlogReactions for how a vote updates these.
  likeCount: number
  dislikeCount: number
}

export interface BlogComment {
  id: string
  name?: string
  content: string
  createdAt: string
}
