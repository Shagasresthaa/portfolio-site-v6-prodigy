/// <reference types="astro/client" />

interface ImportMetaEnv {
  /** Base URL of portfolio-site-v6-prodigy-api. */
  readonly PUBLIC_API_BASE_URL?: string;

  /**
   * Mock password still used by resetPassword()/verifyPassword() in src/stores/auth.ts -
   * no real endpoint for those yet. login() itself is real.
   */
  readonly PUBLIC_ADMIN_PASSWORD?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
