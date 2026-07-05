import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router, { initSiteSettingsTracking } from './router'
import { useThemeStore } from './stores/theme'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Apply the persisted theme (defaulting to light) before mount to avoid a flash of the wrong theme.
useThemeStore(pinia)

// Needs an active Pinia instance, hence after app.use(pinia) above.
initSiteSettingsTracking()

app.mount('#app')
