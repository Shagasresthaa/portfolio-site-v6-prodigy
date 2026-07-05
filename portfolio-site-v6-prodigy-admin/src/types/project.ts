// Mirrors portfolio-site-v6-prodigy-ui's src/types/project.ts - keep in
// sync, this is the shape ProjectCard.vue/ProjectsComponent.vue there expect
// from projects.json. Unlike HighlightItem, there's only one image field -
// no separate thumbnail/full-size distinction for projects.
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
  // Showcase media: an uploaded/converted image or an embedded demo video
  // (videoUrl, e.g. YouTube) - mirrors HighlightItem's mediaType split.
  // Missing/undefined is treated as IMAGE for older saved data (including
  // existing admin-overrides localStorage entries) that predates this field.
  mediaType?: ProjectMediaType
  image?: string
  videoUrl?: string
}
