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
 * Sets document.title and og:/twitter: meta tags, for this tab and any
 * client-rendered preview only - link-preview crawlers fetch raw server HTML
 * and don't execute JS, so real rich previews come from the API's
 * BlogPreviewController instead.
 *
 * No unmount cleanup - router.ts's afterEach resets these to site defaults
 * on every navigation regardless.
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
