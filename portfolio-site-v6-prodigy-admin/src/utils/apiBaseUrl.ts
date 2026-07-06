export function getApiBaseUrl(): string {
  return import.meta.env.PUBLIC_API_BASE_URL || 'http://localhost:8080'
}
