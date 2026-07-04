# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Structure

Monorepo with two independent sub-projects:

- `portfolio-site-v6-prodigy-ui/` — Vue 3 frontend (Vite, TypeScript, Pinia, Vue Router, Tailwind CSS 4)
- `portfolio-site-v6-prodigy-api/` — Spring Boot 4 backend (Java 21, JPA, PostgreSQL)

## UI Commands

All commands run from `portfolio-site-v6-prodigy-ui/` using `pnpm`.

```bash
pnpm install          # install dependencies
pnpm dev              # dev server with HMR
pnpm build            # type-check + production build
pnpm test:unit        # run Vitest unit tests
pnpm test:unit -- --reporter=verbose  # run a single test file: pnpm test:unit src/components/__tests__/HelloWorld.spec.ts
pnpm lint             # oxlint then eslint (both with --fix)
pnpm format           # prettier over src/
pnpm type-check       # vue-tsc only (no emit)
```

Node engine requirement: `^22.18.0 || >=24.12.0`.

`@` is aliased to `src/` in both Vite and TypeScript configs.

## API Commands

All commands run from `portfolio-site-v6-prodigy-api/` using the Gradle wrapper.

```bash
./gradlew bootRun     # start Spring Boot dev server
./gradlew test        # run JUnit 5 tests
./gradlew build       # compile + test + package JAR
./gradlew test --tests "com.sresthaa.SomeTest#methodName"  # run a single test
```

Requires Java 21. The app expects a running PostgreSQL instance configured externally.

## Architecture

**Frontend** — Vue 3 Composition API with `<script setup>`. Styling via Tailwind CSS 4 (`@tailwindcss/vite` plugin) with `@headlessui/vue`, `@heroicons/vue`, and `@fortawesome/vue-fontawesome` (solid + brands icon sets). State via Pinia stores (`src/stores/theme.ts` — light/dark theme, persisted to `localStorage`, toggles the `.dark` class on `<html>`; custom color tokens for both modes are defined via `@theme` in `src/assets/main.css`). Routing via Vue Router 5 with web history (`src/router/index.ts`). Views live in `src/views/`, reusable components in `src/components/`.

Routes: `/` (Home), `/projects`, `/blog`, `/highlights`, `/contact` — each a thin View that wraps a same-named Component (e.g. `ContactView.vue` renders `ContactComponent.vue`), following the existing `HomeView`/`HomeComponent` pattern. `NavbarComponent`'s `navigation` array drives both the nav links (`RouterLink`, desktop + mobile) and `route.meta.title` per route. Projects/Blog/Highlights are still placeholders (just the page name); Contact and Home have real UI.

`HomeComponent.vue` is ported from the old Next.js site: a hero `<section id="home">` (profile picture, greeting, name, bio lines, social icon row via `@fortawesome/free-brands-svg-icons`, animated scroll-down chevron that smooth-scrolls to `#about`) followed by an `<section id="about">` (intro blurb, story paragraphs, and `TimelineWheel.vue` for the career history). All the old site's hardcoded `text-white`/inline `--color-text-*` styles were replaced with the semantic tokens (`text-ink`/`text-ink-muted`, accent color → `text-primary`). The hero uses `min-h-[calc(100vh-4rem)]` rather than `min-h-screen` — the old site's Navbar was a floating overlay that didn't consume layout space, but ours is `sticky` and takes up a real `h-16` (4rem), so a plain `min-h-screen` hero would overshoot the first viewport by that much and push the scroll-down button below the fold.

**Dynamic content pattern** — `HomeComponent.vue`'s About text and career timeline are loaded from `public/data/home.json` via `src/composables/useCachedResource.ts`, a generic `useCachedResource<T>(key, url, ttlMs)` that checks a timestamped `localStorage` entry first (default TTL: 1 week) and only `fetch()`s if stale/missing, falling back to stale cache on fetch failure rather than showing nothing. `public/data/*.json` is gitignored (`portfolio-site-v6-prodigy-ui/.gitignore`) since it's explicitly a stand-in for the real API, not tracked content — it won't exist on a fresh clone. When the Spring Boot API is ready, swapping this is a one-line change (`useCachedResource('home-content', '/api/home')`), same composable, same caching behavior. Reuse this composable for Projects/Blog/Highlights instead of writing new fetch/cache logic per page.

**`TimelineWheel.vue`** (career history, work in progress) — a rotating radial dial rather than a static list: events sit at even angles around a full circle (`360 / count` apart), only spread across `chronological.value` (oldest-first, so it reads left-to-right as it rotates). Only labels within `VISIBLE_RANGE` degrees of the top are shown (rest are conceptually "around the back", faded via `opacity`/`pointer-events`); `rotation` increases continuously via a `requestAnimationFrame` loop, so different years drift through a **stationary** glass window (`border-primary/60 bg-primary/25 backdrop-blur-md`) fixed at the arc's topmost point. Interactions: hovering a visible label eases (spring-lerp, not a hard jump) the rotation so that label's angle converges on 0 (into the window), pausing auto-rotate until the mouse leaves; dragging (`pointerdown`/`pointermove`/`pointerup` on `window`, not just the element, so drags continue even if the pointer leaves the component bounds) directly maps horizontal movement to rotation, overriding both auto-rotate and hover-ease while held. The nearest-to-0 label is also magnified (`scale` ramping 1x→`MAX_SCALE` within `MAGNIFY_RANGE`), like it's being read through a magnifying glass. Shown from `md` up (`hidden md:block`, 768px+) with a plain stacked-card fallback below that — chosen over the default `lg` (1024px) because that cut off iPad-class tablets in portrait (e.g. iPad 10th-gen at 820px wide) even though there's enough width for the arc; below `md` the geometry genuinely doesn't translate. The active-event summary text (date/title/position/institution below the wheel) is gated behind the same `hidden md:block` breakpoint rather than always rendered, since it's a readout paired with the wheel and looks like orphaned clutter stacked above the mobile cards otherwise. All geometry constants are in **rem**, not px, including the ones fed into inline `transform`/`left`/`top` styles — rem is the standard unit for anything that should track the user's root font-size.

Gotchas hit building this one (worth knowing before touching it again):
- **Don't share `transform-origin` between rotate and scale.** The label's rotate (tangent-to-arc lean) uses `origin-top`; when the magnify `scale()` was on the *same* element, scaling around a top-pinned origin made the box grow downward from that fixed edge, so the text's visual center bobbed up/down continuously as it entered/left the magnify zone. Fixed by splitting into a wrapper (`<button>`, handles position + rotate, `origin-top`) and an inner `<span>` (handles `scale()` only, default centered origin).
- **`backdrop-blur` blurs whatever's *behind* it, including your own text if it's behind too.** The glass window was originally `z-10` (on top of the labels), which blurred the currently-active label's own text into an unreadable smear. Fixed by rendering the window *before* the labels in the DOM (so it sits behind them) — the blur now only softens the page background, not the number passing in front of it.
- **Don't snap an "active by proximity" element's position to a fixed target.** To fix a minor magnify-scale misalignment, the active label's rendered position was briefly forced to exactly match the window's coordinates for as long as it stayed "nearest to 0" — which visibly froze it in place for several seconds during auto-rotation instead of letting it glide through. Reverted; positions must stay driven purely by the continuous per-frame angle, never overridden by "closest to X" logic.
- A wide angle sweep (e.g. the dome-shaped predecessor of this component used `±80°`) makes the cosine curve flat on top and steep at the ends, reading as lopsided rather than a smooth arc — keep sweeps/visible-ranges narrower and gentler.

`BackgroundComponent.vue` (also ported from the old Next.js site) renders `public/assets/telescope.svg`/`laptop.svg`/`astronaut.svg` as a `fixed -z-10 pointer-events-none` background — telescope+laptop split top/bottom on the left (hidden below `md`), astronaut on the right (always visible). It's `fixed` to the viewport (not `absolute` inside `<main>`) so it stays static and doesn't stretch on tall pages (e.g. `HomeComponent.vue`'s hero+About content) — it renders behind the translucent/blurred Navbar and Footer rather than being clipped by them, since both already use `bg-surface/80 backdrop-blur-md`. It uses independent `top`/`right`/`bottom`/`left` offsets (currently more inset from the top than the other sides) rather than a symmetric `inset-*` so the illustration block can be nudged in any direction without fighting the other sides; the rotated+scaled laptop graphic (`-rotate-18 scale-[0.65]`) needs enough inset margin or its rotated bounding box clips against the container's `overflow-hidden` edge. The SVGs are solid black glyphs with no color tokens of their own, so it's rendered at low opacity (`opacity-10`) with `dark:invert` to flip them white in dark mode — adjust that pair, not the SVG files, if it needs to read more/less strongly.

`App.vue` uses a flex sticky-footer layout (`flex min-h-screen flex-col`) so `NavbarComponent` and `FooterComponent` always sit at the top/bottom of the viewport regardless of page content length. `<main>` is `flex w-full flex-1 justify-center` — because of this, a view's root element is a flex item and will **not** stretch to fill available width on its own; give it `w-full` explicitly (alongside any `max-w-*`) if it needs to actually reach that max width instead of shrink-wrapping its content (see `ContactComponent.vue`). Static assets (imported from the previous Next.js portfolio) live in `public/assets/`.

**Fonts** — carried over from the old portfolio, loaded via Google Fonts `@import` in `src/assets/main.css` and exposed as `@theme` tokens (which Tailwind 4 auto-generates matching `font-*` utilities for). Usage convention: `--font-salsa` (`font-salsa`) is the default for **most** UI text — applied to the whole `<nav>` in `NavbarComponent` and to `ContactComponent`'s form (labels, inputs, button); `--font-kalam` (`font-kalam`) is reserved for spots that want **more style** — the footer copyright and the Contact page subtitle. `--font-sans` (Geist) is only the raw fallback set on `body` in `base.css`, not meant to be reached for directly in components. `Charm` and `Pacifico` existed in the old site's CSS but were never actually used, so they were not ported. The font `@import` lines must stay **before** `@import 'tailwindcss'` in `main.css` — Tailwind's import expands into real CSS rules at that position, and a plain `@import` placed after any non-import rule in the compiled stylesheet is invalid per the CSS spec and gets silently dropped by the browser.

Also note: form controls (`input`/`textarea`/`button`) don't reliably inherit the ambient font in every browser even with Tailwind's preflight `font: inherit` rule — set the font utility explicitly on the control itself (as `ContactComponent.vue` does) rather than relying on inheritance from an ancestor.

**Color tokens** — `src/assets/main.css` defines a semantic palette via `@theme` (`--color-surface`/`-muted`, `--color-ink`/`-muted`, `--color-primary` (Sky), `--color-secondary` (Lime — reserved for CTAs only), `--color-warning` (Amber), `--color-danger` (Red)), each with light/dark values (dark overrides in the `.dark` block). All components (`NavbarComponent`, `FooterComponent`, `ContactComponent`) are built against these tokens now — use them (`bg-surface`, `text-ink`/`text-ink-muted`, `border-ink-muted/20`, etc.) instead of raw Tailwind `gray-*` classes for anything theme-aware. This matters beyond consistency: Tailwind v4's default `gray` scale is defined in OKLCH with a ~265° (blue-violet) hue, so it reads with a visible cool/blue tint next to the app's true-neutral `surface`/`ink` tokens — this caused a visible color mismatch in the Navbar/Footer dark mode before they were migrated.

**Theme default** — `useThemeStore` (`src/stores/theme.ts`) defaults to **light** on first visit (not the OS `prefers-color-scheme`); it only switches to dark if the user has explicitly toggled it before (persisted to `localStorage`).

**Cascade layers gotcha** — the global reset in `src/assets/base.css` (`box-sizing`/`margin` on `*`) must stay **inside** `@layer base`. Per the CSS Cascade Layers spec, unlayered styles form an implicit final layer that outranks every named layer regardless of selector specificity, so an unlayered `* { margin: 0 }` would silently override every Tailwind margin utility (`mb-*`, `mt-*`, etc.) app-wide. This exact bug happened once already — if margin/padding utilities mysteriously stop applying, check this first before assuming the utility class is wrong.

**Backend** — Spring Boot 4 with Spring MVC (`spring-boot-starter-webmvc`) and Spring Data JPA backed by PostgreSQL (configured on port 5430). Base package is `com.sresthaa`; domain code lives under `com.sresthaa.publicui` with the following layers scaffolded but **empty** (class/interface declaration only, no fields/methods/annotations yet — only `HeartbeatController` has real logic):
- **Controllers**: `HeartbeatController` (implemented), `BlogController`, `ContactFormController`, `GalleryController`, `ProjectController`
- **Models**: `Blog`, `ContactForm`, `Gallery`, `Project`
- **Repositories**: `BlogRepository`, `ContactFormRepository`, `GalleryRepository`, `ProjectRepository`
- **Services**: `BlogService`, `ContactFormService`, `GalleryService`, `ProjectService`

Because of this, `ContactComponent.vue`'s form submit is currently local-only (resets the form, shows a local success message) — there's no real endpoint to call yet.

**Test environment** — UI tests run in jsdom. API tests use Spring Boot test slices (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`).
