<template>
  <button
    v-if="certificates && certificates.length > 0"
    type="button"
    class="text-ink-muted hover:text-primary transition-all duration-300 hover:scale-110"
    aria-label="Certificates"
    @click="showGallery = true"
  >
    <FontAwesomeIcon :icon="faAward" class="text-2xl md:text-3xl" />
  </button>

  <!-- Gallery - a centered, 75%-viewport-wide grid, not a small anchored dropdown -->
  <TransitionRoot :show="showGallery" as="template">
    <Dialog as="div" class="relative z-50" @close="showGallery = false">
      <TransitionChild
        as="template"
        enter="duration-200 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-150 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="bg-ink/70 fixed inset-0 backdrop-blur-sm" />
      </TransitionChild>

      <div class="fixed inset-0 flex items-center justify-center p-4">
        <TransitionChild
          as="template"
          enter="duration-200 ease-out"
          enter-from="scale-95 opacity-0"
          enter-to="scale-100 opacity-100"
          leave="duration-150 ease-in"
          leave-from="scale-100 opacity-100"
          leave-to="scale-95 opacity-0"
        >
          <DialogPanel
            class="bg-surface font-salsa relative flex max-h-[85vh] w-[75vw] flex-col overflow-hidden rounded-2xl shadow-2xl"
          >
            <div class="border-ink-muted/20 flex items-center justify-between border-b p-4">
              <span class="text-lg font-semibold">Certificates</span>
              <button
                type="button"
                class="text-ink-muted hover:text-ink transition-colors"
                aria-label="Close"
                @click="showGallery = false"
              >
                <FontAwesomeIcon :icon="faXmark" class="size-5" aria-hidden="true" />
              </button>
            </div>

            <div class="grid grid-cols-2 gap-4 overflow-y-auto p-6 sm:grid-cols-3 lg:grid-cols-4">
              <button
                v-for="certificate in certificates"
                :key="certificate.id"
                type="button"
                class="border-ink-muted/20 bg-surface-muted overflow-hidden rounded-lg border transition-transform hover:scale-105"
                @click="selected = certificate"
              >
                <img
                  :src="certificate.imageUrl"
                  :alt="certificate.title ?? ''"
                  class="aspect-square w-full object-cover"
                />
                <p v-if="certificate.title" class="text-ink-muted truncate p-2 text-left text-xs">
                  {{ certificate.title }}
                </p>
              </button>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>

  <!-- Lightbox - the gallery's thumbnails are still smallish, this shows one at full size -->
  <TransitionRoot :show="selected !== null" as="template">
    <Dialog as="div" class="relative z-[60]" @close="selected = null">
      <TransitionChild
        as="template"
        enter="duration-200 ease-out"
        enter-from="opacity-0"
        enter-to="opacity-100"
        leave="duration-150 ease-in"
        leave-from="opacity-100"
        leave-to="opacity-0"
      >
        <div class="bg-ink/70 fixed inset-0 backdrop-blur-sm" />
      </TransitionChild>

      <div class="fixed inset-0 flex items-center justify-center p-4">
        <TransitionChild
          as="template"
          enter="duration-200 ease-out"
          enter-from="scale-95 opacity-0"
          enter-to="scale-100 opacity-100"
          leave="duration-150 ease-in"
          leave-from="scale-100 opacity-100"
          leave-to="scale-95 opacity-0"
        >
          <DialogPanel class="bg-surface relative w-full max-w-2xl overflow-hidden rounded-2xl shadow-2xl">
            <button
              type="button"
              class="bg-ink/50 hover:bg-ink/70 text-surface absolute top-4 right-4 z-10 rounded-full p-2 transition-colors"
              aria-label="Close"
              @click="selected = null"
            >
              <FontAwesomeIcon :icon="faXmark" class="size-5" aria-hidden="true" />
            </button>

            <img
              v-if="selected"
              :src="selected.imageUrl"
              :alt="selected.title ?? ''"
              class="max-h-[70vh] w-full object-contain"
            />

            <div v-if="selected?.title" class="border-ink-muted/20 bg-surface-muted border-t p-4 text-center">
              <p class="font-kalam text-ink-muted text-sm italic">{{ selected.title }}</p>
            </div>
          </DialogPanel>
        </TransitionChild>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Dialog, DialogPanel, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { faAward, faXmark } from '@fortawesome/free-solid-svg-icons'
import { useCachedResource } from '@/composables/useCachedResource'
import { getApiBaseUrl } from '@/utils/apiBaseUrl'

interface Certificate {
  id: string
  imageUrl: string
  title?: string
  createdAt: string
}

const { data: certificates } = useCachedResource<Certificate[]>('certificates', `${getApiBaseUrl()}/api/certificates`)

const showGallery = ref(false)
const selected = ref<Certificate | null>(null)
</script>
