// Mirrors portfolio-site-v6-prodigy-ui's src/types/blog.ts - keep in sync,
// this is the shape BlogCard.vue/BlogPostComponent.vue there expect from
// blog.json.
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
  // Baseline counts simulating other visitors - the public UI layers the
  // current browser's own vote on top of these (see useBlogReactions there).
  // The admin doesn't edit these directly; they're carried through as-is.
  likeCount: number
  dislikeCount: number
}
