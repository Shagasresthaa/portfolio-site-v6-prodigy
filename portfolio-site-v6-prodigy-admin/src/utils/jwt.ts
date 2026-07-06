// Decodes a JWT's `exp` claim without verifying the signature - the server already
// verifies signatures on every request; this is only used to know when to stop
// sending an obviously-expired token.
export function decodeJwtExpiryMs(token: string): number {
  const payload = token.split('.')[1]
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4)
  const claims = JSON.parse(atob(padded)) as { exp: number }
  return claims.exp * 1000
}
