import { ref } from 'vue'

const COPIED_RESET_MS = 2000

/** Native share sheet when available, else copy-to-clipboard + a brief `justCopied` flag. */
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
