export type HighlightMediaType = 'IMAGE' | 'VIDEO'

export interface HighlightItem {
  id: string
  title: string
  description?: string
  caption?: string
  mediaType: HighlightMediaType
  // IMAGE only: thumbnailImage is the grid card, image is the full-size modal view.
  thumbnailImage?: string
  image?: string
  // VIDEO only.
  videoUrl?: string
  // Comma-separated, matching the old site's storage format.
  tags: string
  createdAt: string
}
