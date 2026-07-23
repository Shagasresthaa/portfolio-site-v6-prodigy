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
  // Comma-separated, matching the old site's storage format.
  techStacks: string
  projectUrl?: string
  liveUrl?: string
  // Screenshot (image) or embedded demo video (videoUrl). Undefined = IMAGE (pre-dates this field).
  mediaType?: ProjectMediaType
  image?: string
  videoUrl?: string
}
