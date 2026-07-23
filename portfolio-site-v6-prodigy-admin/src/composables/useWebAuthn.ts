// Wraps native WebAuthn L3 JSON methods - pair directly with webauthn4j's base64url (de)serialization, no manual ArrayBuffer conversion needed.

export class WebAuthnUnsupportedError extends Error {
  constructor() {
    super('This browser does not support security keys/passkeys (WebAuthn).')
    this.name = 'WebAuthnUnsupportedError'
  }
}

function assertSupported() {
  if (
    typeof PublicKeyCredential === 'undefined' ||
    typeof PublicKeyCredential.parseCreationOptionsFromJSON !== 'function' ||
    typeof PublicKeyCredential.parseRequestOptionsFromJSON !== 'function'
  ) {
    throw new WebAuthnUnsupportedError()
  }
}

// Ceremonies are user-initiated (button click) and can be cancelled/time out - surface
// that as a plain, expected message rather than a raw DOMException.
function friendlyMessage(err: unknown, action: string): Error {
  if (err instanceof WebAuthnUnsupportedError) return err
  if (err instanceof DOMException && err.name === 'NotAllowedError') {
    return new Error(`${action} was cancelled or timed out.`)
  }
  return err instanceof Error ? err : new Error(`Failed to ${action.toLowerCase()}.`)
}

export async function registerSecurityKey(optionsJSON: PublicKeyCredentialCreationOptionsJSON): Promise<string> {
  assertSupported()
  try {
    const publicKey = PublicKeyCredential.parseCreationOptionsFromJSON(optionsJSON)
    const credential = await navigator.credentials.create({ publicKey })
    if (!(credential instanceof PublicKeyCredential)) throw new Error('Registration was not completed.')
    return JSON.stringify(credential.toJSON())
  } catch (err) {
    throw friendlyMessage(err, 'Registration')
  }
}

export async function assertSecurityKey(optionsJSON: PublicKeyCredentialRequestOptionsJSON): Promise<string> {
  assertSupported()
  try {
    const publicKey = PublicKeyCredential.parseRequestOptionsFromJSON(optionsJSON)
    const credential = await navigator.credentials.get({ publicKey })
    if (!(credential instanceof PublicKeyCredential)) throw new Error('Sign-in was not completed.')
    return JSON.stringify(credential.toJSON())
  } catch (err) {
    throw friendlyMessage(err, 'Sign-in')
  }
}
