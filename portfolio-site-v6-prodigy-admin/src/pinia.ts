import { createPinia } from 'pinia'
import type { App } from 'vue'

/**
 * @astrojs/vue mounts every `client:*` island as its own isolated Vue app
 * instance, so Pinia needs to be installed once per island rather than
 * once globally - this file is wired up as `appEntrypoint` in
 * astro.config.mjs specifically so that happens automatically.
 */
export default (app: App) => {
  app.use(createPinia())
}
