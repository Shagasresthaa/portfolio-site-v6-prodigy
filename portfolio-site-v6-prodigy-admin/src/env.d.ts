/// <reference types="astro/client" />

interface ImportMetaEnv {
  /** Base URL of portfolio-site-v6-prodigy-api. */
  readonly PUBLIC_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
