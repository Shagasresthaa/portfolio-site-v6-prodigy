// Mirrors the UI's src/types/highlight.ts - keep in sync.
export type HighlightMediaType = 'IMAGE' | 'VIDEO'

export interface HighlightItem {
  id: string
  title: string
  description?: string
  caption?: string
  mediaType: HighlightMediaType
  thumbnailImage?: string // IMAGE only: grid-card thumbnail.
  image?: string // IMAGE only: full-size, shown in the modal.
  videoUrl?: string // VIDEO only.
  tags: string // Comma-separated.
  createdAt: string
}
