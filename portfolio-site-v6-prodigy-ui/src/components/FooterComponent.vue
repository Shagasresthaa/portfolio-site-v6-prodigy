<template>
  <footer
    class="font-sans border-ink-muted/20 bg-surface/80 mt-auto w-full border-t backdrop-blur-md"
  >
    <div
      class="flex w-full flex-row flex-nowrap items-center justify-center gap-2 px-4 py-6 sm:gap-4 sm:px-6 md:gap-6 lg:gap-8 lg:px-8"
    >
      <a
        href="https://www.digitalocean.com/?refcode=85f458f92f16&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge"
        target="_blank"
        rel="noopener noreferrer"
        class="shrink-0 transition-transform hover:scale-105"
      >
        <img
          :src="digitalOceanBadge"
          alt="DigitalOcean Referral Badge"
          class="h-6 w-auto sm:h-7 md:h-9 lg:h-11"
        />
      </a>

      <img
        src="/assets/postgresql-logo.svg"
        alt="Powered by PostgreSQL"
        class="h-6 w-auto shrink-0 transition-transform hover:scale-105 sm:h-7 md:h-9 lg:h-11"
      />

      <span
        class="font-kalam text-ink-muted text-center text-xs font-bold whitespace-nowrap sm:text-sm md:text-base lg:text-lg"
      >
        &copy; Shaga Sresthaa, {{ currentYear }}
      </span>

      <img
        src="/assets/kubernetes-logo.svg"
        alt="Deployed on Kubernetes"
        class="h-6 w-auto shrink-0 transition-transform hover:scale-105 sm:h-7 md:h-9 lg:h-11"
      />

      <img
        src="/assets/CF_Logo_1.png"
        alt="Secured by Cloudflare"
        class="h-6 w-auto shrink-0 transition-transform hover:scale-105 sm:h-7 md:h-9 lg:h-11"
      />
    </div>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

const currentYear = new Date().getFullYear()

const themeStore = useThemeStore()
const digitalOceanBadge = computed(() =>
  themeStore.theme === 'dark'
    ? 'https://web-platforms.sfo2.cdn.digitaloceanspaces.com/WWW/Badge%202.svg'
    : 'https://web-platforms.sfo2.cdn.digitaloceanspaces.com/WWW/Badge%201.svg',
)
</script>
