// Mirrors portfolio-site-v6-prodigy-ui's src/types/siteSettings.ts - keep in
// sync, this is the shape stores/siteSettings.ts and router/index.ts there
// expect from site-settings.json.
export interface SiteSettings {
  // Tab-title suffix, e.g. "Blog - {siteTitle}".
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
