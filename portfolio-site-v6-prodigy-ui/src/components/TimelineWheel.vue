<template>
  <div class="relative">
    <h2 class="font-salsa text-primary mb-12 text-center text-4xl font-bold md:text-5xl">
      My Journey so far
    </h2>

    <!-- Desktop/tablet (md and up, ~768px+): events sit on an arc that slowly rotates, so different years fly
    into the stationary glass window at the top over time. Hovering a year eases
    the rotation to bring it into that window and pauses auto-rotation until you
    leave. Drag left/right to spin it manually. -->
    <div
      class="relative hidden h-[14rem] w-full touch-none select-none md:block"
      :class="isDragging ? 'cursor-grabbing' : 'cursor-grab'"
      @mouseleave="clearHovered"
      @pointerdown="startDrag"
    >
      <!-- Stationary glass window - the read-point events rotate into. Sits behind -->
      <!-- the labels (not on top) so its backdrop-blur softens the page behind it -->
      <!-- rather than blurring the active year's own text into a smear. -->
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
        <!-- Scale is on its own element with the default (centered) transform-origin, -->
        <!-- separate from the button's origin-top rotation - sharing one origin would -->
        <!-- make the magnified text bob vertically as it grows from a pinned top edge. -->
        <span
          class="font-salsa inline-block text-lg transition-[opacity,color] duration-300"
          :class="i === activeIndex ? 'text-primary font-semibold' : 'text-ink-muted hover:text-primary'"
          :style="{ transform: `scale(${node.scale})` }"
        >
          {{ node.year }}
        </span>
      </button>
    </div>

    <div class="hidden text-center md:block">
      <p class="font-kalam text-ink-muted text-sm">{{ active.date }}</p>
      <h3 class="font-salsa text-primary mb-2 text-2xl font-bold md:text-3xl">
        {{ active.title }}
      </h3>
      <p class="font-kalam font-semibold">{{ active.position }}</p>
      <p class="font-kalam text-ink-muted">{{ active.institution }}</p>
      <p v-if="active.duration" class="text-ink-muted mt-1 text-sm">{{ active.duration }}</p>
    </div>

    <!-- Mobile/tablet fallback: a rotating arc doesn't translate to narrow viewports -->
    <div class="mt-8 space-y-6 md:hidden">
      <div v-for="event in events" :key="event.title" class="bg-surface-muted rounded-lg p-6 shadow-xl">
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

// Horizontal placement is a percentage of the container width so the arc scales
// fluidly. Vertical placement is in rem (not px), the standard unit for anything
// that should track the user's root font-size rather than a hardcoded pixel value.
const CENTER_X_PCT = 50
const RADIUS_X_PCT = 28
const CENTER_Y = 12 // rem
const RADIUS_Y = 7 // rem
// Only labels within this many degrees of the top are shown - the rest are
// conceptually "around the back" of the full circle, faded out until rotation
// brings them into range.
const VISIBLE_RANGE = 75
// The label only tilts a fraction of its arc angle - rotating it the full amount
// makes the numbers hard to read rather than subtly "fanned out".
const LABEL_ROTATION_SCALE = 0.35
// A label is magnified like it's passing under a magnifying glass as it nears
// the window (angle 0), ramping from 1x up to MAX_SCALE within this many degrees.
const MAGNIFY_RANGE = 20
const MAX_SCALE = 1.35
const ROTATION_SPEED_DEG_PER_SEC = 6
const HOVER_EASE = 0.08

// Use the end year for a range ("Dec, 2021 - Nov, 2023" -> 2023), or the only
// year when there's no range ("Jan, 2024" -> 2024, "Mar, 2025 - Present" -> 2025).
function extractYear(date: string): string {
  const matches = date.match(/\d{4}/g)
  return matches ? matches[matches.length - 1]! : date
}

// Oldest first, so the arc reads chronologically as it rotates.
const chronological = computed(() => [...props.events].reverse())

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

  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  rafId = requestAnimationFrame(tick)
})

onUnmounted(() => {
  if (rafId !== undefined) cancelAnimationFrame(rafId)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', stopDrag)
})
</script>
