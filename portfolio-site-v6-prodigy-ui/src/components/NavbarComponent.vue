<template>
  <Disclosure
    as="nav"
    v-slot="{ open }"
    class="font-salsa border-ink-muted/20 bg-surface/80 sticky top-0 z-50 w-full border-b backdrop-blur-md"
  >
    <div class="flex h-16 w-full items-center justify-between px-4 sm:px-6 lg:px-8">
      <RouterLink to="/" class="text-ink text-lg tracking-tight"> {{ route.meta.title }} </RouterLink>

      <div class="flex items-center gap-4">
        <div
          class="relative hidden md:flex md:items-center md:gap-1"
          @mouseleave="hoverIndex = null"
        >
          <span
            class="bg-ink pointer-events-none absolute top-0 left-0 h-full rounded-full transition-[transform,width] duration-300 ease-out"
            :style="indicatorStyle"
          />
          <RouterLink
            v-for="(item, index) in navigation"
            :key="item.name"
            :ref="(el) => setItemRef(el, index)"
            :to="item.href"
            :class="[
              highlightIndex === index ? 'text-surface' : 'text-ink-muted hover:text-ink',
              'relative z-10 flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition-colors',
            ]"
            :aria-current="isCurrent(item) ? 'page' : undefined"
            @mouseenter="hoverIndex = index"
          >
            <FontAwesomeIcon :icon="item.icon" class="size-4" aria-hidden="true" />
            <span>{{ item.name }}</span>
          </RouterLink>
        </div>

        <div class="flex items-center gap-1">
          <button
            type="button"
            class="text-ink-muted hover:bg-ink/5 hover:text-ink focus-visible:bg-ink/5 focus-visible:text-ink relative rounded-full p-2 transition-colors focus-visible:outline-none"
            @click="themeStore.toggleTheme()"
          >
            <span class="sr-only">Toggle theme</span>
            <SunIcon v-if="themeStore.theme === 'dark'" class="size-5" aria-hidden="true" />
            <MoonIcon v-else class="size-5" aria-hidden="true" />
          </button>

          <DisclosureButton
            class="text-ink-muted hover:bg-ink/5 hover:text-ink focus-visible:bg-ink/5 focus-visible:text-ink relative rounded-full p-2 transition-colors focus-visible:outline-none md:hidden"
          >
            <span class="sr-only">Open main menu</span>
            <Bars3Icon v-if="!open" class="size-6" aria-hidden="true" />
            <XMarkIcon v-else class="size-6" aria-hidden="true" />
          </DisclosureButton>
        </div>
      </div>
    </div>

    <DisclosurePanel class="md:hidden">
      <div class="border-ink-muted/20 space-y-1 border-t px-4 pt-2 pb-3">
        <DisclosureButton
          v-for="item in navigation"
          :key="item.name"
          :as="RouterLink"
          :to="item.href"
          :class="[
            isCurrent(item) ? 'bg-ink text-surface' : 'text-ink-muted hover:bg-ink/5 hover:text-ink',
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
import {
  type ComponentPublicInstance,
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  watch,
} from 'vue'
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
  { name: 'Projects', href: '/projects', icon: faFolderOpen },
  { name: 'Blog', href: '/blog', icon: faPenNib },
  { name: 'Highlights', href: '/highlights', icon: faStar },
  { name: 'Contact', href: '/contact', icon: faEnvelope },
]

function isCurrent(item: (typeof navigation)[number]) {
  return item.href === route.path
}

const itemEls = ref<(HTMLElement | null)[]>([])

function setItemRef(el: Element | ComponentPublicInstance | null, index: number) {
  itemEls.value[index] = el === null ? null : (('$el' in el ? el.$el : el) as HTMLElement)
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
