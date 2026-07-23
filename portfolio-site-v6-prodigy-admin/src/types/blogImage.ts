// Mirrors the API's BlogImageSummary - reusable library, uploaded once and referenced by URL across posts.
export interface BlogImage {
  id: string
  url: string
  altText?: string
  createdAt: string
}
