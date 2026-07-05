import { watch, type Ref } from 'vue'

export interface DocumentMeta {
  title: string
  description?: string
  image?: string
  url?: string
}

export function setMetaTag(attr: 'name' | 'property', key: string, content: string) {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

/**
 * Sets document.title and the og:/twitter: meta tags for link previews (used by
 * BlogPostComponent). This only affects this browser's own tab/any client-side
 * preview - it does NOT make sharing the URL produce a real rich preview in
 * iMessage/Discord/WhatsApp/Slack, since those crawlers fetch the raw server
 * HTML and don't execute JS. A real preview needs the backend to serve
 * per-post meta tags in the initial response - deferred until that exists
 * (see CLAUDE.md).
 *
 * No unmount cleanup here - router.ts's afterEach unconditionally resets
 * these same tags to site-wide defaults (from stores/siteSettings.ts) on
 * every navigation, so leaving this route always restores a sane baseline
 * without explicit teardown (same reasoning documented there for why the
 * title itself was never restored on unmount either).
 */
export function useDocumentMeta(meta: Ref<DocumentMeta | null>) {
  watch(
    meta,
    (value) => {
      if (!value) return
      document.title = value.title
      setMetaTag('property', 'og:title', value.title)
      setMetaTag('property', 'og:type', 'article')
      if (value.description) {
        setMetaTag('name', 'description', value.description)
        setMetaTag('property', 'og:description', value.description)
        setMetaTag('name', 'twitter:description', value.description)
      }
      if (value.image) {
        setMetaTag('property', 'og:image', value.image)
        setMetaTag('name', 'twitter:image', value.image)
      }
      if (value.url) setMetaTag('property', 'og:url', value.url)
      setMetaTag('name', 'twitter:card', value.image ? 'summary_large_image' : 'summary')
    },
    { immediate: true },
  )
}
