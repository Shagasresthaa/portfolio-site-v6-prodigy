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

`BackgroundComponent.vue` (ported from the old Next.js site) renders `public/assets/telescope.svg`/`laptop.svg`/`astronaut.svg` as an `absolute inset-0 -z-10 pointer-events-none` background — telescope+laptop split top/bottom on the left (hidden below `md`), astronaut on the right (always visible). It's mounted inside `<main>` (which is `position: relative`), not fixed to the viewport, so it's bounded to exactly the space between Navbar and Footer rather than running full-bleed behind them. The SVGs are solid black glyphs with no color tokens of their own, so it's rendered at low opacity (`opacity-10`) with `dark:invert` to flip them white in dark mode — adjust that pair, not the SVG files, if it needs to read more/less strongly.

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
