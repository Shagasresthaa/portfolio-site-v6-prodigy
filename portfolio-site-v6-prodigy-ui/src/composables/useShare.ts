import { ref } from 'vue'

const COPIED_RESET_MS = 2000

/**
 * Native share sheet when available (mobile Safari/Chrome, most modern
 * browsers), falling back to copy-to-clipboard with a brief "Copied!" flag
 * for the caller to render. Each call site gets its own `justCopied` state,
 * so a grid of cards can each show their own feedback independently.
 */
export function useShare() {
  const justCopied = ref(false)

  async function share(data: { title: string; text?: string; url: string }) {
    if (navigator.share) {
      try {
        await navigator.share(data)
      } catch {
        // User cancelled the native share sheet - not an error.
      }
      return
    }

    await navigator.clipboard.writeText(data.url)
    justCopied.value = true
    setTimeout(() => {
      justCopied.value = false
    }, COPIED_RESET_MS)
  }

  return { share, justCopied }
}
