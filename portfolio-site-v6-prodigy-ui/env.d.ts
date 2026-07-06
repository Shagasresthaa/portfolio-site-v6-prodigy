/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of portfolio-site-v6-prodigy-api. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
