import { type ComponentPublicInstance, nextTick, onMounted, onUnmounted, ref, watch, type WatchSource } from 'vue'

const MOBILE_BREAKPOINT_PX = 768

/**
 * Uniform card height across a grid, as a floor rather than a live CSS stretch:
 * measured once per card set (not per render), so a card growing its own content
 * (e.g. expanding a "Read more") can still grow past the floor without dragging
 * its row-mates along. `itemsSource` should track the rendered card set (e.g. a
 * paginated/filtered list) - recalculation is tied to that changing, not to any
 * per-card content change.
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
    // Reset first so a stale (possibly larger) min-height from a previous card set
    // doesn't inflate this measurement.
    cardMinHeight.value = 0
    await nextTick()
    let max = 0
    cardEls.forEach((el) => {
      max = Math.max(max, el.offsetHeight)
    })
    cardMinHeight.value = max
  }

  // immediate: true matters - useCachedResource can resolve synchronously from
  // localStorage (no await before that assignment when the cache is fresh), so
  // itemsSource is sometimes already populated before this watcher is even set
  // up, meaning it would never see a "change" to trigger on without this.
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
