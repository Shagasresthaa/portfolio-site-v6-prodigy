<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-4xl px-4 py-16 font-salsa">
      <div class="mb-8">
        <h1 class="text-3xl">Contact Messages</h1>
        <p v-if="unreadCount > 0" class="text-ink-muted mt-1 text-sm">
          {{ unreadCount }} unread message{{ unreadCount !== 1 ? 's' : '' }}
        </p>
      </div>

      <p v-if="loading" class="text-ink-muted text-center">Loading messages…</p>
      <p v-else-if="error" class="text-danger text-center">{{ error }}</p>

      <div v-else-if="messages.length > 0" class="flex flex-col gap-4">
        <div
          v-for="entry in messages"
          :key="entry.id"
          :class="[
            entry.read ? 'border-ink-muted/20 bg-surface-muted/50' : 'border-primary/50 bg-primary-soft',
            'rounded-2xl border p-6 transition-colors',
          ]"
        >
          <div class="mb-4 flex items-start justify-between gap-4">
            <div class="flex items-start gap-3">
              <EnvelopeOpenIcon v-if="entry.read" class="text-ink-muted mt-1 size-5 shrink-0" aria-hidden="true" />
              <EnvelopeIcon v-else class="text-primary mt-1 size-5 shrink-0" aria-hidden="true" />
              <div>
                <p class="font-semibold">{{ entry.name || 'Anonymous' }}</p>
                <p class="text-ink-muted text-sm">{{ entry.email }}</p>
                <p v-if="entry.subject" class="mt-2 font-semibold">Subject: {{ entry.subject }}</p>
                <p class="text-ink-muted mt-1 text-xs">{{ formatDate(entry.createdAt) }}</p>
              </div>
            </div>

            <div class="flex shrink-0 gap-2">
              <button
                v-if="!entry.read"
                type="button"
                class="bg-secondary-soft text-secondary rounded-lg p-2 transition hover:opacity-80"
                title="Mark as read"
                @click="handleMarkAsRead(entry.id)"
              >
                <CheckIcon class="size-4" aria-hidden="true" />
              </button>
              <button
                type="button"
                class="bg-danger-soft text-danger rounded-lg p-2 transition hover:opacity-80"
                title="Delete"
                @click="handleDelete(entry)"
              >
                <TrashIcon class="size-4" aria-hidden="true" />
              </button>
            </div>
          </div>

          <div class="bg-surface rounded-lg p-4">
            <p class="whitespace-pre-wrap">{{ entry.message }}</p>
          </div>
        </div>
      </div>

      <div
        v-else
        class="border-ink-muted/30 flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-12"
      >
        <p class="text-ink-muted text-xl">No messages yet</p>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { CheckIcon, EnvelopeIcon, EnvelopeOpenIcon, TrashIcon } from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { useContactMessages, type ContactMessage } from '@/composables/useContactMessages'

const { messages, unreadCount, loading, error, refresh, markAsRead, deleteMessage } = useContactMessages()

onMounted(refresh)

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

async function handleMarkAsRead(id: string) {
  try {
    await markAsRead(id)
  } catch {
    // Non-critical - the admin can just retry the click.
  }
}

async function handleDelete(entry: ContactMessage) {
  if (!confirm(`Delete message from ${entry.email}?`)) return
  try {
    await deleteMessage(entry.id)
  } catch {
    // Non-critical - the admin can just retry the click.
  }
}
</script>
