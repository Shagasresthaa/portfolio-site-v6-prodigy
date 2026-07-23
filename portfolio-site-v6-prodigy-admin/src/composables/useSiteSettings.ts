import type { SiteSettings } from '@/types/siteSettings'

const OVERRIDE_KEY = 'admin-site-settings-override'
const BASE_URL = '/data/site-settings.json'

/** Mocked locally (override-over-baseline, like useHomeContent.ts) - no backend endpoint exists yet. */
export function readSiteSettingsOverride(): SiteSettings | null {
  const raw = localStorage.getItem(OVERRIDE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SiteSettings
  } catch {
    return null
  }
}

export function saveSiteSettingsOverride(settings: SiteSettings) {
  localStorage.setItem(OVERRIDE_KEY, JSON.stringify(settings))
}

export function clearSiteSettingsOverride() {
  localStorage.removeItem(OVERRIDE_KEY)
}

export async function fetchBaseSiteSettings(): Promise<SiteSettings> {
  const response = await fetch(BASE_URL)
  if (!response.ok) throw new Error(`Failed to fetch ${BASE_URL}: ${response.status}`)
  return (await response.json()) as SiteSettings
}

/** Override if one's been saved, otherwise the baseline JSON. */
export async function loadSiteSettings(): Promise<{ settings: SiteSettings; isDraft: boolean }> {
  const override = readSiteSettingsOverride()
  if (override) return { settings: override, isDraft: true }
  return { settings: await fetchBaseSiteSettings(), isDraft: false }
}
