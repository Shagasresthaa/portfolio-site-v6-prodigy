// Mirrors the UI's src/types/siteSettings.ts - keep in sync.
export interface SiteSettings {
  siteTitle: string // Tab-title suffix, e.g. "Blog - {siteTitle}".
  defaultDescription: string // Meta/OG fallback for pages with no per-entity content.
  defaultShareImage?: string // og:image/twitter:image fallback for those same pages.
  searchIndexingEnabled: boolean // false emits noindex,nofollow site-wide.
}
