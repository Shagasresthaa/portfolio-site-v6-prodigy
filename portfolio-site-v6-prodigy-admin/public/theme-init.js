// Every navigation is a fresh server-rendered page (not an SPA), so without this the page
// always paints light first and only flips to dark once the Navbar's Vue island hydrates and
// stores/theme.ts runs - this applies the class synchronously, before first paint, to avoid
// that flash. Storage key must match STORAGE_KEY there.
//
// Extracted out of Layout.astro into its own same-origin file (rather than an inline <script>)
// so `script-src 'self'` covers it without needing a CSP hash that breaks every time this file
// is edited.
try {
  if (localStorage.getItem('theme') === 'dark') {
    document.documentElement.classList.add('dark')
  }
} catch {}
