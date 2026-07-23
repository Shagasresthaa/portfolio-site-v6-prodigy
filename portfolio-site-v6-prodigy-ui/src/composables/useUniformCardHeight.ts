import { type ComponentPublicInstance, nextTick, onMounted, onUnmounted, ref, watch, type WatchSource } from 'vue'

const MOBILE_BREAKPOINT_PX = 768

/**
 * Uniform card height as a min-height floor, not a live CSS stretch - measured
 * once per card *set* change (e.g. new page/filter), not per render, so one
 * card growing its own content doesn't drag its row-mates taller.
 */
export function useUniformCardHeight(itemsSource: WatchSource<unknown[]>) {
  const cardMinHeight = ref(0)
  const cardEls = new Map<string, HTMLElement>()

  function setCardRef(id: string, el: Element | ComponentPublicInstance | null) {
    if (el === null) {
      cardEls.delete(id)
      return
    }
    cardEls.set(id, ('$el' in el ? el.$el : el) as HTMLElement)
  }

  async function recalcCardMinHeight() {
    if (window.innerWidth < MOBILE_BREAKPOINT_PX || cardEls.size === 0) {
      cardMinHeight.value = 0
      return
    }
    // Reset first so a stale, larger min-height doesn't inflate this measurement.
    cardMinHeight.value = 0
    await nextTick()
    let max = 0
    cardEls.forEach((el) => {
      max = Math.max(max, el.offsetHeight)
    })
    cardMinHeight.value = max
  }

  // immediate: true required - useCachedResource can resolve synchronously from a
  // warm cache, so itemsSource may already be populated before this watcher exists.
  watch(itemsSource, () => nextTick(recalcCardMinHeight), { immediate: true })

  let resizeTimeoutId: ReturnType<typeof setTimeout> | undefined
  function handleResize() {
    if (resizeTimeoutId !== undefined) clearTimeout(resizeTimeoutId)
    resizeTimeoutId = setTimeout(recalcCardMinHeight, 150)
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (resizeTimeoutId !== undefined) clearTimeout(resizeTimeoutId)
  })

  return { cardMinHeight, setCardRef }
}
