import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'

import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

interface BlogPostSample {
  slug: string
  published: boolean
  title: string
  excerpt: string
  coverImage?: string
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * Dev-server-only aid for testing link previews locally, without waiting on
 * the real Spring Boot backend (see CLAUDE.md's "Link previews are a real
 * technical wrinkle" note). `curl`/link-preview crawlers don't execute JS, so
 * they never see BlogPostComponent's client-side useDocumentMeta tags - this
 * middleware injects the same tags server-side, into the raw HTML, for any
 * fresh (non-SPA-client-routed) request to /blog/:slug. Pair with a tunnel
 * (ngrok/cloudflared) pointed at the dev server to test against real external
 * tools (opengraph.xyz, Discord, iMessage) without deploying anywhere.
 *
 * This is NOT the production fix - `apply: 'serve'` means it never runs in a
 * built/deployed site, which would need this same idea implemented server-side
 * in Spring Boot instead.
 */
function blogPostMetaDevPlugin(): Plugin {
  return {
    name: 'blog-post-meta-dev-only',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        const match = req.url?.match(/^\/blog\/([^/?#]+)/)
        if (!match) return next()

        const dataPath = fileURLToPath(new URL('./public/data/blog.json', import.meta.url))
        if (!fs.existsSync(dataPath)) return next()

        const slug = decodeURIComponent(match[1]!)
        const { posts } = JSON.parse(fs.readFileSync(dataPath, 'utf-8')) as { posts: BlogPostSample[] }
        const post = posts.find((p) => p.slug === slug && p.published)
        if (!post) return next()

        const indexPath = fileURLToPath(new URL('./index.html', import.meta.url))
        const rawHtml = fs.readFileSync(indexPath, 'utf-8')
        const html = await server.transformIndexHtml(req.url!, rawHtml, req.originalUrl)

        const origin = `${req.headers['x-forwarded-proto'] ?? 'http'}://${req.headers.host}`
        const title = post.title
        const description = post.excerpt
        const image = post.coverImage
        const url = `${origin}/blog/${post.slug}`

        const metaTags = [
          `<meta name="description" content="${escapeHtml(description)}">`,
          `<meta property="og:type" content="article">`,
          `<meta property="og:title" content="${escapeHtml(title)}">`,
          `<meta property="og:description" content="${escapeHtml(description)}">`,
          `<meta property="og:url" content="${escapeHtml(url)}">`,
          image ? `<meta property="og:image" content="${escapeHtml(new URL(image, origin).toString())}">` : '',
          `<meta name="twitter:card" content="${image ? 'summary_large_image' : 'summary'}">`,
        ]
          .filter(Boolean)
          .join('\n    ')

        const withMeta = html
          .replace(/<title>.*?<\/title>/, `<title>${escapeHtml(title)}</title>`)
          .replace('</head>', `    ${metaTags}\n  </head>`)

        res.setHeader('Content-Type', 'text/html')
        res.end(withMeta)
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools(), tailwindcss(), blogPostMetaDevPlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
