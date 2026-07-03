<template>
  <footer
    class="font-sans mt-auto w-full border-t border-gray-200 bg-white/80 backdrop-blur-md dark:border-white/10 dark:bg-gray-900/80"
  >
    <div class="flex w-full flex-col items-center gap-4 px-4 py-6 sm:px-6 lg:px-8">
      <!-- Top row: DigitalOcean | Copyright | Cloudflare -->
      <div class="flex flex-row flex-wrap items-center justify-center gap-4 md:gap-8">
        <a
          href="https://www.digitalocean.com/?refcode=85f458f92f16&utm_campaign=Referral_Invite&utm_medium=Referral_Program&utm_source=badge"
          target="_blank"
          rel="noopener noreferrer"
          class="transition-transform hover:scale-105"
        >
          <img
            src="https://web-platforms.sfo2.cdn.digitaloceanspaces.com/WWW/Badge%202.svg"
            alt="DigitalOcean Referral Badge"
            class="h-6 w-auto md:h-9"
          />
        </a>

        <span
          class="font-kalam text-center text-sm font-medium text-gray-600 md:text-base dark:text-gray-400"
        >
          &copy; Shaga Sresthaa, {{ currentYear }}
        </span>

        <img
          src="/assets/CF_Logo_1.png"
          alt="Secured by Cloudflare"
          class="h-5 w-auto transition-transform hover:scale-105 md:h-7"
        />
      </div>

      <!-- Bottom row: Docker logo -->
      <img
        src="/assets/docker-logo-blue.svg"
        alt="Powered by Docker"
        class="h-5 w-auto transition-transform hover:scale-105 md:h-6"
      />
    </div>
  </footer>
</template>

<script setup lang="ts">
const currentYear = new Date().getFullYear()
</script>
