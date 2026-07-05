/// <reference types="astro/client" />

interface ImportMetaEnv {
  /**
   * Mock-auth credentials used until the Spring Boot API exposes a real
   * `/api/admin/auth/login` endpoint - see src/stores/auth.ts.
   */
  readonly PUBLIC_ADMIN_USERNAME?: string;
  readonly PUBLIC_ADMIN_PASSWORD?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
