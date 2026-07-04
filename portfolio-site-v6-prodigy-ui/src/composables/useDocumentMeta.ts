import { onUnmounted, watch, type Ref } from 'vue'

export interface DocumentMeta {
  title: string
  description?: string
  image?: string
  url?: string
}

const MANAGED_META_ATTR = 'data-managed-meta'

function setMetaTag(attr: 'name' | 'property', key: string, content: string) {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    el.setAttribute(MANAGED_META_ATTR, 'true')
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

function removeManagedMetaTags() {
  document.head.querySelectorAll(`[${MANAGED_META_ATTR}]`).forEach((el) => el.remove())
}

/**
 * Sets document.title and the og:/twitter: meta tags for link previews (used by
 * BlogPostComponent). This only affects this browser's own tab/any client-side
 * preview - it does NOT make sharing the URL produce a real rich preview in
 * iMessage/Discord/WhatsApp/Slack, since those crawlers fetch the raw server
 * HTML and don't execute JS. A real preview needs the backend to serve
 * per-post meta tags in the initial response - deferred until that exists
 * (see CLAUDE.md).
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

  // Only the injected meta tags need explicit cleanup here - the title doesn't,
  // since Vue Router's global afterEach guard (see router/index.ts) always runs
  // *before* the destination route's component mounts, so restoring a
  // "previous title" here would fire after and stomp back over whatever title
  // the guard already (correctly) set for wherever navigation is heading next.
  onUnmounted(removeManagedMetaTags)
}
