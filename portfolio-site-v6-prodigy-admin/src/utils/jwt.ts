// Decodes `exp` without verifying the signature - only used to stop sending an obviously-expired token; server verifies signatures itself.
export function decodeJwtExpiryMs(token: string): number {
  const payload = token.split('.')[1]
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
  const claims = JSON.parse(atob(padded)) as { exp: number }
  return claims.exp * 1000
}
