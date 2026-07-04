export type HighlightMediaType = 'IMAGE' | 'VIDEO'

export interface HighlightItem {
  id: string
  title: string
  description?: string
  caption?: string
  mediaType: HighlightMediaType
  // IMAGE only: thumbnailImage is what the card grid shows, image is the
  // full-size version shown in the modal when the thumbnail is clicked.
  thumbnailImage?: string
  image?: string
  // VIDEO only.
  videoUrl?: string
  // Comma-separated, matching the old site's storage format.
  tags: string
  createdAt: string
}
