<template>
  <div class="relative">
    <h2 class="font-salsa text-primary mb-12 text-center text-4xl font-bold md:text-5xl">
      My Journey so far
    </h2>

    <!-- Desktop/tablet (md+): events rotate on an arc into the stationary window at top.
    Hover eases a year into view and pauses auto-rotate; drag to spin manually. -->
    <div
      class="relative hidden h-[14rem] w-full touch-none select-none md:block"
      :class="isDragging ? 'cursor-grabbing' : 'cursor-grab'"
      @mouseleave="clearHovered"
      @pointerdown="startDrag"
    >
      <!-- Behind the labels, not on top - backdrop-blur must not blur the active label's own text. -->
      <div
        class="border-primary/60 bg-primary/25 pointer-events-none absolute min-w-44 -translate-x-1/2 -translate-y-1/2 rounded-xl border-2 py-8 text-center backdrop-blur-md"
        :style="{ left: `${CENTER_X_PCT}%`, top: `${CENTER_Y - RADIUS_Y}rem` }"
      />

      <button
        v-for="(node, i) in nodes"
        :key="node.title"
        type="button"
        class="will-change-transform absolute origin-top rounded-lg px-1 py-1 whitespace-nowrap"
        :style="{
          left: `${node.xPct}%`,
          top: `${node.y}rem`,
          transform: `translate(-50%, -50%) rotate(${node.labelAngleDeg}deg)`,
          opacity: node.visible ? 1 : 0,
          pointerEvents: node.visible ? 'auto' : 'none',
        }"
        :aria-label="node.title"
        @mouseenter="setHovered(i)"
      >
        <!-- Scale on its own element (centered origin) - sharing the button's origin-top would bob the text as it magnifies. -->
        <span
          class="font-salsa inline-block text-lg transition-[opacity,color] duration-300"
          :class="i === activeIndex ? 'text-primary font-semibold' : 'text-ink-muted hover:text-primary'"
          :style="{ transform: `scale(${node.scale})` }"
        >
          {{ node.year }}
        </span>
      </button>
    </div>

    <!-- out-in avoids two different-length text blocks overlapping mid-transition. -->
    <Transition
      mode="out-in"
      enter-active-class="transition-opacity duration-200"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div :key="activeIndex" class="hidden text-center md:block">
        <p class="font-kalam text-ink-muted text-sm">{{ active.date }}</p>
        <h3 class="font-salsa text-primary mb-2 text-2xl font-bold md:text-3xl">
          {{ active.title }}
        </h3>
        <p class="font-kalam font-semibold">{{ active.position }}</p>
        <p class="font-kalam text-ink-muted">{{ active.institution }}</p>
        <p v-if="active.duration" class="font-salsa text-ink-muted mt-1 text-sm">{{ active.duration }}</p>
      </div>
    </Transition>

    <!-- Mobile fallback: auto-scrolling horizontal cards instead of the arc. List is duplicated
    back-to-back for a seamless loop - scroll position wraps by one set's width. -->
    <div
      ref="scrollerRef"
      class="[scrollbar-width:none] [&::-webkit-scrollbar]:hidden mt-8 flex gap-6 overflow-x-auto px-[7.5%] md:hidden"
      @pointerdown="startMobileScrollInteraction"
      @scrollend="endMobileScrollInteraction"
    >
      <div
        v-for="(event, i) in loopedEvents"
        :key="`${event.title}-${i}`"
        class="bg-surface-muted/75 w-[85%] shrink-0 rounded-lg p-6 shadow-xl sm:w-80"
      >
        <h3 class="font-salsa text-primary mb-2 text-xl font-bold">{{ event.title }}</h3>
        <p class="font-kalam mb-1 text-lg font-semibold">{{ event.position }}</p>
        <p class="font-kalam text-ink-muted mb-2 text-base">{{ event.institution }}</p>
        <div class="font-salsa text-ink-muted flex flex-wrap gap-2 text-sm">
          <span>{{ event.date }}</span>
          <template v-if="event.duration">
            <span>&bull;</span>
            <span>{{ event.duration }}</span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { TimelineEvent } from '@/types/timeline'

const props = defineProps<{ events: TimelineEvent[] }>()

interface Node extends TimelineEvent {
  year: string
  currentAngle: number
  labelAngleDeg: number
  xPct: number
  y: number
  visible: boolean
  scale: number
}

// Horizontal in %, vertical in rem (tracks root font-size).
const CENTER_X_PCT = 50
const RADIUS_X_PCT = 28
const CENTER_Y = 12 // rem
const RADIUS_Y = 7 // rem
// Labels beyond this many degrees from top are "around the back", faded out.
const VISIBLE_RANGE = 75
// Fraction of arc angle the label tilts - full angle makes numbers unreadable.
const LABEL_ROTATION_SCALE = 0.35
// Label magnifies from 1x to MAX_SCALE within this many degrees of angle 0.
const MAGNIFY_RANGE = 20
const MAX_SCALE = 1.35
const ROTATION_SPEED_DEG_PER_SEC = 6
const HOVER_EASE = 0.08
// How fast the mobile horizontal scroller drifts, in px/sec.
const MOBILE_AUTO_SCROLL_SPEED_PX_PER_SEC = 40

// Last 4-digit year in the string (end year of a range, or the only year).
function extractYear(date: string): string {
  const matches = date.match(/\d{4}/g)
  return matches ? matches[matches.length - 1]! : date
}

// Oldest first, so the arc reads chronologically as it rotates.
const chronological = computed(() => [...props.events].reverse())

// Duplicated so the mobile scroller can loop seamlessly - see template comment.
const loopedEvents = computed(() => [...props.events, ...props.events])

function normalizeAngle(deg: number): number {
  let a = deg % 360
  if (a > 180) a -= 360
  if (a < -180) a += 360
  return a
}

const rotation = ref(0)
const hoveredIndex = ref<number | null>(null)
const isDragging = ref(false)

// How many degrees the wheel turns per pixel of horizontal mouse movement.
const DRAG_SENSITIVITY = 0.3
let dragStartX = 0
let dragStartRotation = 0

const nodes = computed<Node[]>(() => {
  const items = chronological.value
  const count = items.length
  const step = 360 / count
  return items.map((event, i) => {
    const currentAngle = normalizeAngle(i * step + rotation.value)
    const angleRad = (currentAngle * Math.PI) / 180
    const proximity = Math.max(0, 1 - Math.abs(currentAngle) / MAGNIFY_RANGE)
    return {
      ...event,
      year: extractYear(event.date),
      currentAngle,
      labelAngleDeg: currentAngle * LABEL_ROTATION_SCALE,
      xPct: CENTER_X_PCT + RADIUS_X_PCT * Math.sin(angleRad),
      y: CENTER_Y - RADIUS_Y * Math.cos(angleRad),
      visible: Math.abs(currentAngle) <= VISIBLE_RANGE,
      scale: 1 + proximity * (MAX_SCALE - 1),
    }
  })
})

// Whichever event is currently closest to the framed slot at the top.
const activeIndex = computed(() => {
  let bestIndex = 0
  let bestAbsAngle = Infinity
  nodes.value.forEach((node, i) => {
    const absAngle = Math.abs(node.currentAngle)
    if (absAngle < bestAbsAngle) {
      bestAbsAngle = absAngle
      bestIndex = i
    }
  })
  return bestIndex
})
const active = computed(() => nodes.value[activeIndex.value]!)

function setHovered(i: number) {
  hoveredIndex.value = i
}

function clearHovered() {
  hoveredIndex.value = null
}

function onPointerMove(event: PointerEvent) {
  rotation.value = dragStartRotation + (event.clientX - dragStartX) * DRAG_SENSITIVITY
}

function stopDrag() {
  isDragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', stopDrag)
}

function startDrag(event: PointerEvent) {
  isDragging.value = true
  hoveredIndex.value = null
  dragStartX = event.clientX
  dragStartRotation = rotation.value
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', stopDrag)
}

const scrollerRef = ref<HTMLElement | null>(null)
const isMobileScrollInteracting = ref(false)

// Tracked separately, not read back from scrollLeft - some browsers round scrollLeft
// to an integer, which truncates sub-pixel/frame increments to zero forever.
let mobileScrollPosition = 0

// Fallback only - normal resume is via the `scrollend` event. Resuming on pointerup
// instead would cancel the swipe's native momentum scroll while it's still settling.
const INTERACTION_TIMEOUT_MS = 3000
let interactionTimeoutId: ReturnType<typeof setTimeout> | undefined

function endMobileScrollInteraction() {
  isMobileScrollInteracting.value = false
  if (interactionTimeoutId !== undefined) {
    clearTimeout(interactionTimeoutId)
    interactionTimeoutId = undefined
  }
}

function startMobileScrollInteraction() {
  isMobileScrollInteracting.value = true
  if (interactionTimeoutId !== undefined) clearTimeout(interactionTimeoutId)
  interactionTimeoutId = setTimeout(endMobileScrollInteraction, INTERACTION_TIMEOUT_MS)
}

let rafId: number | undefined
let lastTime: number | undefined

function tick(time: number) {
  if (lastTime === undefined) lastTime = time
  const deltaSeconds = (time - lastTime) / 1000
  lastTime = time

  if (isDragging.value) {
    // Position is driven directly by the pointer while dragging.
  } else if (hoveredIndex.value !== null) {
    // Ease the arc so the hovered event's angle converges on 0 (the framed slot).
    const node = nodes.value[hoveredIndex.value]
    if (node) rotation.value -= node.currentAngle * HOVER_EASE
  } else {
    rotation.value -= ROTATION_SPEED_DEG_PER_SEC * deltaSeconds
  }

  // clientWidth is 0 when hidden (md+ viewports), so this no-ops on desktop.
  const scroller = scrollerRef.value
  if (scroller && scroller.clientWidth > 0) {
    if (isMobileScrollInteracting.value) {
      // Stay in sync with manual scroll so autoscroll resumes without a jump.
      mobileScrollPosition = scroller.scrollLeft
    } else {
      const loopWidth = scroller.scrollWidth / 2
      if (loopWidth > 0) {
        mobileScrollPosition = (mobileScrollPosition + MOBILE_AUTO_SCROLL_SPEED_PX_PER_SEC * deltaSeconds) % loopWidth
        scroller.scrollLeft = mobileScrollPosition
      }
    }
  }

  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  rafId = requestAnimationFrame(tick)
})

onUnmounted(() => {
  if (rafId !== undefined) cancelAnimationFrame(rafId)
  if (interactionTimeoutId !== undefined) clearTimeout(interactionTimeoutId)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', stopDrag)
})
</script>
