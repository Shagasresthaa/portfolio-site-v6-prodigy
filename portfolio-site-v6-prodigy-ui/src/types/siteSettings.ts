export interface SiteSettings {
  // Tab-title suffix, e.g. "Blog - {siteTitle}" (see router/index.ts).
  siteTitle: string
  // Fallback meta/OG description for pages with no per-entity content of their own.
  defaultDescription: string
  // Fallback og:image/twitter:image for those same pages.
  defaultShareImage?: string
  // Site-wide robots directive - false emits noindex,nofollow everywhere.
  searchIndexingEnabled: boolean
}
