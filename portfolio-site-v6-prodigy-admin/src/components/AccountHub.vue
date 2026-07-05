<template>
  <AuthGate>
    <div class="mx-auto w-full max-w-3xl px-4 py-16 font-salsa">
      <div class="mb-8 text-center">
        <h1 class="mb-2 text-3xl">Account</h1>
        <p class="text-ink-muted text-sm">
          Signed in as <span class="text-ink font-semibold">{{ authStore.username }}</span>
        </p>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <a
          v-for="card in cards"
          :key="card.href"
          :href="card.href"
          class="border-ink-muted/20 bg-surface-muted/70 hover:border-(--color-primary) flex flex-col gap-3 rounded-2xl border p-6 transition"
        >
          <component :is="card.icon" class="text-primary size-6" aria-hidden="true" />
          <div>
            <h2 class="text-lg">{{ card.title }}</h2>
            <p class="text-ink-muted text-sm">{{ card.description }}</p>
          </div>
        </a>
      </div>
    </div>
  </AuthGate>
</template>

<script setup lang="ts">
import {
  DevicePhoneMobileIcon,
  FingerPrintIcon,
  KeyIcon,
  UserIcon,
} from '@heroicons/vue/24/outline'
import AuthGate from '@/components/AuthGate.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const cards = [
  {
    href: '/account/password',
    title: 'Reset password',
    description: 'Change the password used to sign in.',
    icon: KeyIcon,
  },
  {
    href: '/account/username',
    title: 'Change username',
    description: 'Update the username used to sign in.',
    icon: UserIcon,
  },
  {
    href: '/account/security-keys',
    title: 'Security keys',
    description: 'Manage hardware keys (YubiKey, etc.) for passwordless sign-in via WebAuthn.',
    icon: FingerPrintIcon,
  },
  {
    href: '/account/2fa',
    title: 'Two-factor authentication',
    description: 'Require an authenticator app code at login.',
    icon: DevicePhoneMobileIcon,
  },
]
</script>
