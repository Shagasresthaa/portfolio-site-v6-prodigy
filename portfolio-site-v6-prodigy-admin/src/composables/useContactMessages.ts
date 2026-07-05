import { computed, ref, type Ref } from 'vue'

export interface ContactMessage {
  id: string
  name: string | null
  email: string
  subject: string | null
  message: string
  read: boolean
  createdAt: string
}

const OVERRIDES_KEY = 'admin-contact-overrides'

interface Override {
  read?: boolean
  deleted?: boolean
}

function readOverrides(): Record<string, Override> {
  if (typeof localStorage === 'undefined') return {}
  const raw = localStorage.getItem(OVERRIDES_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, Override>
  } catch {
    return {}
  }
}

/**
 * Mark-as-read/delete are mocked locally - there's no ContactFormController
 * endpoint yet (see CLAUDE.md). Overrides layer on top of the sample data
 * from contact.json exactly like useBlogReactions.ts (in the UI project)
 * layers likes/dislikes on top of post data, so refreshing the page doesn't
 * lose read/deleted state even though the underlying "API" is a static file.
 */
export function useContactMessages(baseMessages: Ref<ContactMessage[] | null>) {
  const overrides = ref<Record<string, Override>>(readOverrides())

  const messages = computed(() => {
    const base = baseMessages.value ?? []
    return base
      .filter((message) => !overrides.value[message.id]?.deleted)
      .map((message) => ({
        ...message,
        read: overrides.value[message.id]?.read ?? message.read,
      }))
  })

  const unreadCount = computed(() => messages.value.filter((message) => !message.read).length)

  function persist() {
    localStorage.setItem(OVERRIDES_KEY, JSON.stringify(overrides.value))
  }

  function markAsRead(id: string) {
    overrides.value = { ...overrides.value, [id]: { ...overrides.value[id], read: true } }
    persist()
  }

  function deleteMessage(id: string) {
    overrides.value = { ...overrides.value, [id]: { ...overrides.value[id], deleted: true } }
    persist()
  }

  return { messages, unreadCount, markAsRead, deleteMessage }
}
