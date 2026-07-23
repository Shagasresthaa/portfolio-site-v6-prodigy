import { defineStore } from 'pinia'
import { computed } from 'vue'
import { useCachedResource } from '@/composables/useCachedResource'
import type { SiteSettings } from '@/types/siteSettings'

// Fallback until site-settings.json loads (or if it fails).
const DEFAULT_SITE_TITLE = 'Shaga Sresthaa'

export const useSiteSettingsStore = defineStore('siteSettings', () => {
  const { data } = useCachedResource<SiteSettings>('site-settings', '/data/site-settings.json')

  const siteTitle = computed(() => data.value?.siteTitle || DEFAULT_SITE_TITLE)
  const defaultDescription = computed(() => data.value?.defaultDescription ?? '')
  const defaultShareImage = computed(() => data.value?.defaultShareImage)
  const searchIndexingEnabled = computed(() => data.value?.searchIndexingEnabled ?? true)

  return { siteTitle, defaultDescription, defaultShareImage, searchIndexingEnabled }
})
