<template>
  <Disclosure
    as="nav"
    v-slot="{ open }"
    class="sticky top-0 z-50 w-full border-b border-gray-200 bg-white/80 backdrop-blur-md dark:border-white/10 dark:bg-gray-900/80"
  >
    <div class="flex h-16 w-full items-center justify-between px-4 sm:px-6 lg:px-8">
      <RouterLink
        to="/"
        class="text-lg font-semibold tracking-tight text-gray-900 dark:text-white"
      >
        {{ route.meta.title }}
      </RouterLink>

      <div class="flex items-center gap-4">
        <div
          class="relative hidden md:flex md:items-center md:gap-1"
          @mouseleave="hoverIndex = null"
        >
          <span
            class="pointer-events-none absolute top-0 left-0 h-full rounded-full bg-gray-900 transition-[transform,width] duration-300 ease-out dark:bg-white"
            :style="indicatorStyle"
          />
          <a
            v-for="(item, index) in navigation"
            :key="item.name"
            :ref="(el) => setItemRef(el, index)"
            :href="item.href"
            :class="[
              highlightIndex === index
                ? 'text-white dark:text-gray-900'
                : 'text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white',
              'relative z-10 flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition-colors',
            ]"
            :aria-current="isCurrent(item) ? 'page' : undefined"
            @mouseenter="hoverIndex = index"
          >
            <FontAwesomeIcon :icon="item.icon" class="size-4" aria-hidden="true" />
            <span>{{ item.name }}</span>
          </a>
        </div>

        <div class="flex items-center gap-1">
          <button
            type="button"
            class="relative rounded-full p-2 text-gray-500 transition-colors hover:bg-gray-900/5 hover:text-gray-900 focus-visible:bg-gray-900/5 focus-visible:text-gray-900 focus-visible:outline-none dark:text-gray-400 dark:hover:bg-white/10 dark:hover:text-white dark:focus-visible:bg-white/10 dark:focus-visible:text-white"
            @click="themeStore.toggleTheme()"
          >
            <span class="sr-only">Toggle theme</span>
            <SunIcon v-if="themeStore.theme === 'dark'" class="size-5" aria-hidden="true" />
            <MoonIcon v-else class="size-5" aria-hidden="true" />
          </button>

          <DisclosureButton
            class="relative rounded-full p-2 text-gray-500 transition-colors hover:bg-gray-900/5 hover:text-gray-900 focus-visible:bg-gray-900/5 focus-visible:text-gray-900 focus-visible:outline-none md:hidden dark:text-gray-400 dark:hover:bg-white/10 dark:hover:text-white dark:focus-visible:bg-white/10 dark:focus-visible:text-white"
          >
            <span class="sr-only">Open main menu</span>
            <Bars3Icon v-if="!open" class="size-6" aria-hidden="true" />
            <XMarkIcon v-else class="size-6" aria-hidden="true" />
          </DisclosureButton>
        </div>
      </div>
    </div>

    <DisclosurePanel class="md:hidden">
      <div class="space-y-1 border-t border-gray-200 px-4 pt-2 pb-3 dark:border-white/10">
        <DisclosureButton
          v-for="item in navigation"
          :key="item.name"
          as="a"
          :href="item.href"
          :class="[
            isCurrent(item)
              ? 'bg-gray-900 text-white dark:bg-white dark:text-gray-900'
              : 'text-gray-600 hover:bg-gray-900/5 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-white/10 dark:hover:text-white',
            'flex items-center gap-2 rounded-lg px-3 py-2 text-base font-medium transition-colors',
          ]"
          :aria-current="isCurrent(item) ? 'page' : undefined"
        >
          <FontAwesomeIcon :icon="item.icon" class="size-4" aria-hidden="true" />
          <span>{{ item.name }}</span>
        </DisclosureButton>
      </div>
    </DisclosurePanel>
  </Disclosure>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/vue'
import { Bars3Icon, MoonIcon, SunIcon, XMarkIcon } from '@heroicons/vue/24/outline'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import {
  faEnvelope,
  faFolderOpen,
  faHouse,
  faPenNib,
  faStar,
} from '@fortawesome/free-solid-svg-icons'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const route = useRoute()

const navigation = [
  { name: 'Home', href: '/', icon: faHouse },
  { name: 'Projects', href: '#', icon: faFolderOpen },
  { name: 'Blog', href: '#', icon: faPenNib },
  { name: 'Highlights', href: '#', icon: faStar },
  { name: 'Contact', href: '#', icon: faEnvelope },
]

function isCurrent(item: (typeof navigation)[number]) {
  return item.href === route.path
}

const itemEls = ref<(HTMLElement | null)[]>([])

function setItemRef(el: Element | { $el: Element } | null, index: number) {
  itemEls.value[index] = (el as HTMLElement | null) ?? null
}

const hoverIndex = ref<number | null>(null)
const activeIndex = computed(() => {
  const index = navigation.findIndex(isCurrent)
  return index === -1 ? null : index
})
const highlightIndex = computed(() => hoverIndex.value ?? activeIndex.value)

const indicatorStyle = ref({ opacity: '0', transform: 'translateX(0px)', width: '0px' })

function updateIndicator() {
  const index = highlightIndex.value
  if (index === null) {
    indicatorStyle.value = { ...indicatorStyle.value, opacity: '0' }
    return
  }
  const el = itemEls.value[index]
  if (!el) return
  indicatorStyle.value = {
    opacity: '1',
    transform: `translateX(${el.offsetLeft}px)`,
    width: `${el.offsetWidth}px`,
  }
}

watch(highlightIndex, () => nextTick(updateIndicator))

function handleResize() {
  updateIndicator()
}

onMounted(() => {
  nextTick(updateIndicator)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>
