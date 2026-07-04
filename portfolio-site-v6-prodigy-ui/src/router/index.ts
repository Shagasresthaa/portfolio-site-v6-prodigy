import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProjectsView from '../views/ProjectsView.vue'
import BlogView from '../views/BlogView.vue'
import BlogPostView from '../views/BlogPostView.vue'
import HighlightsView from '../views/HighlightsView.vue'
import ContactView from '../views/ContactView.vue'

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

// Baseline tab title per route. BlogPostComponent overrides this further with
// post-specific title/OG tags once its data loads (see useDocumentMeta) -
// this guard still matters for the brief moment before that, and for every
// other route, which have no per-entity title of their own.
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - Shaga Sresthaa` : 'Shaga Sresthaa'
})

export default router
