import { computed, ref } from 'vue'
import { authFetch } from '@/composables/useApi'

export interface ContactMessage {
  id: string
  name: string | null
  email: string
  subject: string | null
  message: string
  read: boolean
  createdAt: string
}

export function useContactMessages() {
  const messages = ref<ContactMessage[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const unreadCount = computed(() => messages.value.filter((message) => !message.read).length)

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const response = await authFetch('/api/admin/contact')
      if (!response.ok) throw new Error('Failed to load messages.')
      messages.value = (await response.json()) as ContactMessage[]
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to load messages.'
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(id: string) {
    const response = await authFetch(`/api/admin/contact/${id}/read`, { method: 'POST' })
    if (!response.ok) throw new Error('Failed to mark message as read.')
    const target = messages.value.find((message) => message.id === id)
    if (target) target.read = true
  }

  async function deleteMessage(id: string) {
    const response = await authFetch(`/api/admin/contact/${id}`, { method: 'DELETE' })
    if (!response.ok) throw new Error('Failed to delete message.')
    messages.value = messages.value.filter((message) => message.id !== id)
  }

  return { messages, unreadCount, loading, error, refresh, markAsRead, deleteMessage }
}
