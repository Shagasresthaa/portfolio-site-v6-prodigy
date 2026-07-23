// Mirrors the UI's src/types/project.ts - keep in sync. Unlike HighlightItem, one image field only, no thumbnail.
export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'COMPLETED' | 'MAINTAINED' | 'ARCHIVED'
export type CollabMode = 'SOLO' | 'GROUP'
export type AffiliationType = 'INDEPENDENT' | 'UNIVERSITY' | 'ORGANIZATION' | 'CLUB'
export type SourceCodeAvailability = 'OPEN_SOURCE' | 'CLOSED_SOURCE' | 'UNDER_NDA'
export type ProjectMediaType = 'IMAGE' | 'VIDEO'

export interface Project {
  id: string
  name: string
  shortDesc: string
  longDesc?: string
  statusFlag: ProjectStatus
  startDate: string
  endDate?: string
  collabMode: CollabMode
  affiliation: string
  affiliationType: AffiliationType
  sourceCodeAvailability: SourceCodeAvailability
  techStacks: string // Comma-separated.
  projectUrl?: string
  liveUrl?: string
  mediaType?: ProjectMediaType // Missing/undefined treated as IMAGE (data predating this field).
  image?: string
  videoUrl?: string
}
