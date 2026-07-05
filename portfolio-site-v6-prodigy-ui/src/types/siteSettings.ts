export interface SiteSettings {
  // Tab-title suffix, e.g. "Blog - {siteTitle}" (see router/index.ts).
  siteTitle: string
  // Fallback meta/OG description for pages with no content of their own to
  // pull from (Home/Projects/Highlights/Contact - only blog posts currently
  // have per-page descriptions).
  defaultDescription: string
  // Fallback og:image/twitter:image for those same pages.
  defaultShareImage?: string
  // Site-wide robots directive - false emits noindex,nofollow everywhere.
  // Useful while this is still in active dev, not meant to stay off in prod.
  searchIndexingEnabled: boolean
}
