<template>
  <div class="w-full">
    <!-- Hero Section -->
    <section
      id="home"
      class="relative flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center px-4 text-center"
    >
      <img
        src="/assets/propic.jpg"
        alt="Shaga Sresthaa"
        class="mb-6 h-48 w-48 rounded-full object-cover shadow-2xl ring-4 ring-black/10 transition-transform duration-300 hover:scale-105 md:h-45 md:w-45 dark:ring-white/20"
      />

      <h2
        class="font-salsa text-ink-muted mb-2 text-xl font-light tracking-wide sm:text-2xl md:text-xl"
      >
        Hello There! I'm
      </h2>
      <h1
        class="font-salsa text-primary mb-6 text-4xl font-bold tracking-wide sm:text-5xl md:text-4xl"
      >
        Sresthaa
      </h1>

      <div class="font-kalam mb-6 max-w-7xl space-y-1 text-base sm:text-lg md:text-2xl lg:text-2xl">
        <p>CS Master's Graduate @ Western Michigan University (Class of 2026)</p>
        <p>TCS Alumni || WMU Devclub Alumni</p>
        <p>Full Stack Developer || Astronomy Enthusiast</p>
      </div>

      <div class="mb-12 flex flex-wrap items-center justify-center gap-4 md:gap-6">
        <a
          v-for="link in socialLinks"
          :key="link.label"
          :href="link.href"
          target="_blank"
          rel="noopener noreferrer"
          class="text-ink-muted hover:text-primary transition-all duration-300 hover:scale-110"
          :aria-label="link.label"
        >
          <FontAwesomeIcon :icon="link.icon" class="size-7 md:size-8" />
        </a>
      </div>

      <p class="font-salsa text-ink-muted">Scroll down to find out more!</p>

      <button
        type="button"
        class="group absolute bottom-8 animate-bounce"
        aria-label="Scroll to About section"
        @click="scrollToAbout"
      >
        <div class="flex flex-col items-center gap-2">
          <FontAwesomeIcon
            :icon="faChevronDown"
            class="text-ink-muted group-hover:text-primary size-8 transition-all duration-300"
          />
          <span
            class="font-kalam text-ink-muted text-sm opacity-0 transition-opacity duration-300 group-hover:opacity-100"
          >
            Find out more!
          </span>
        </div>
      </button>
    </section>

    <!-- About Me Section -->
    <section id="about" class="px-4 py-20 md:px-8 lg:px-16">
      <div v-if="homeContent" class="mx-auto max-w-6xl">
        <div class="mb-12 flex flex-col items-center gap-8 md:flex-row md:gap-12">
          <img
            src="/assets/about.jpg"
            alt="About Me"
            class="h-64 w-64 rounded-lg object-cover shadow-2xl ring-4 ring-black/10 md:h-50 md:w-50 dark:ring-white/20"
          />
          <p
            class="font-kalam text-primary text-justify text-xl leading-relaxed font-bold md:text-xl"
          >
            {{ homeContent.aboutHook }}
          </p>
        </div>

        <div class="font-kalam mb-16 space-y-6 text-lg leading-relaxed md:text-xl">
          <p v-for="(paragraph, index) in homeContent.aboutStory" :key="index" class="text-justify indent-12">
            {{ paragraph }}
          </p>
        </div>

        <TimelineWheel :events="homeContent.timeline" />
      </div>

      <p v-else class="text-ink-muted text-center">Loading...</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faAward, faChevronDown, faFilePdf } from '@fortawesome/free-solid-svg-icons'
import { faGithub, faLinkedin, faSquareGitlab, faSteam } from '@fortawesome/free-brands-svg-icons'
import { useCachedResource } from '@/composables/useCachedResource'
import TimelineWheel from './TimelineWheel.vue'
import type { TimelineEvent } from '@/types/timeline'

interface HomeContent {
  aboutHook: string
  aboutStory: string[]
  timeline: TimelineEvent[]
}

const { data: homeContent } = useCachedResource<HomeContent>('home-content', '/data/home.json')

const socialLinks = [
  {
    href: 'https://www.linkedin.com/in/sresthaa-shaga/',
    icon: faLinkedin,
    label: 'LinkedIn',
  },
  {
    href: 'https://github.com/Shagasresthaa',
    icon: faGithub,
    label: 'GitHub',
  },
  {
    href: 'https://gitlab.com/sresthaa-shaga',
    icon: faSquareGitlab,
    label: 'GitLab',
  },
  {
    href: 'https://drive.google.com/file/d/13PJt4xrrdotOcVET3wA536C4uIQ-rJI0/view?usp=sharing',
    icon: faFilePdf,
    label: 'Resume',
  },
  {
    href: 'https://drive.google.com/drive/folders/1bs98W8ON9Rfk_zj_zmxD6dYAP14xQQht?usp=sharing',
    icon: faAward,
    label: 'Awards',
  },
  {
    href: 'https://steamcommunity.com/id/SentinelNS_05/',
    icon: faSteam,
    label: 'Steam',
  },
]

function scrollToAbout() {
  document.getElementById('about')?.scrollIntoView({ behavior: 'smooth' })
}
</script>
