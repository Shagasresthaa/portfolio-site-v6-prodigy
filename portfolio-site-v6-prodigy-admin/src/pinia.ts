import { createPinia } from 'pinia'
import type { App } from 'vue'

/** Wired as `appEntrypoint` in astro.config.mjs - each `client:*` island is its own Vue app, so Pinia installs per-island. */
export default (app: App) => {
  app.use(createPinia())
}
