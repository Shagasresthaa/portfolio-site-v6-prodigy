// Mirrors portfolio-site-v6-prodigy-api's BlogImageSummary - a reusable image library for blog
// post content, uploaded once and referenced (by URL, pasted into markdown) across posts.
export interface BlogImage {
  id: string
  url: string
  altText?: string
  createdAt: string
}
