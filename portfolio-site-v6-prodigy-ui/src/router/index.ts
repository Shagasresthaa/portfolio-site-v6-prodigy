import { createRouter, createWebHistory } from 'vue-router'
import { watch } from 'vue'
import HomeView from '../views/HomeView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import BlogView from '../views/BlogView.vue'
import BlogPostView from '../views/BlogPostView.vue'
import HighlightsView from '../views/HighlightsView.vue'
import ContactView from '../views/ContactView.vue'
import { setMetaTag } from '@/composables/useDocumentMeta'
import { useSiteSettingsStore } from '@/stores/siteSettings'

declare module 'vue-router' {
  interface RouteMeta {
    title: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: 'Home' },
    },
    {
      path: '/projects',
      name: 'projects',
      component: ProjectsView,
      meta: { title: 'Projects' },
    },
    {
      path: '/blog',
      name: 'blog',
      component: BlogView,
      meta: { title: 'Blog' },
    },
    {
      path: '/blog/:slug',
      name: 'blog-post',
      component: BlogPostView,
      meta: { title: 'Blog' },
    },
    {
      path: '/highlights',
      name: 'highlights',
      component: HighlightsView,
      meta: { title: 'Highlights' },
    },
    {
      path: '/contact',
      name: 'contact',
      component: ContactView,
      meta: { title: 'Contact' },
    },
  ],
})

/**
 * Baseline title + meta tags per route, driven by stores/siteSettings.ts.
 * BlogPostComponent overrides these further with post-specific values once
 * its data loads (see useDocumentMeta) - this still matters for the brief
 * moment before that, and for every other route (Home/Projects/Highlights/
 * Contact), which have no per-entity content of their own to pull from.
 *
 * Re-applying unconditionally on every navigation (rather than once at boot,
 * or relying on the previous page's cleanup) is deliberate: afterEach runs
 * *before* the outgoing route's component unmounts, so if that component
 * doesn't restore anything on unmount (useDocumentMeta doesn't), the values
 * set here are still exactly what's left in place afterward - no explicit
 * teardown needed on the way out.
 */
function applyBaselineMeta(pageTitle: string) {
  const store = useSiteSettingsStore()
  document.title = pageTitle ? `${pageTitle} - ${store.siteTitle}` : store.siteTitle

  setMetaTag('property', 'og:title', document.title)
  setMetaTag('property', 'og:type', 'website')
  setMetaTag('name', 'description', store.defaultDescription)
  setMetaTag('property', 'og:description', store.defaultDescription)
  setMetaTag('name', 'twitter:description', store.defaultDescription)
  if (store.defaultShareImage) {
    setMetaTag('property', 'og:image', store.defaultShareImage)
    setMetaTag('name', 'twitter:image', store.defaultShareImage)
  }
  setMetaTag('name', 'twitter:card', store.defaultShareImage ? 'summary_large_image' : 'summary')
  setMetaTag('name', 'robots', store.searchIndexingEnabled ? 'index, follow' : 'noindex, nofollow')
}

router.afterEach((to) => {
  applyBaselineMeta(to.meta.title ?? '')
})

/**
 * Site settings load asynchronously and can resolve after the initial
 * afterEach already ran with fallback values - this re-applies the baseline
 * for whichever route is current once real data arrives, rather than
 * waiting for the next navigation to pick it up. Must be called after
 * `app.use(pinia)` (see main.ts), since it needs an active Pinia instance.
 */
export function initSiteSettingsTracking() {
  const store = useSiteSettingsStore()
  watch(
    () => [store.siteTitle, store.defaultDescription, store.defaultShareImage, store.searchIndexingEnabled],
    () => applyBaselineMeta(router.currentRoute.value.meta.title ?? ''),
  )
}

export default router
