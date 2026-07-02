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

**Frontend** — Vue 3 Composition API with `<script setup>`. Styling via Tailwind CSS 4 (`@tailwindcss/vite` plugin) with `@headlessui/vue` and `@heroicons/vue`. State via Pinia stores (`src/stores/` — directory exists, no stores created yet). Routing via Vue Router 5 with web history (`src/router/index.ts`). Views live in `src/views/`, reusable components in `src/components/`. Currently only the `/` (home) route exists.

**Backend** — Spring Boot 4 with Spring MVC (`spring-boot-starter-webmvc`) and Spring Data JPA backed by PostgreSQL (configured on port 5430). Base package is `com.sresthaa`; domain code lives under `com.sresthaa.publicui` with the following implemented layers:
- **Controllers**: `HeartbeatController`, `BlogController`, `ContactFormController`, `GalleryController`, `ProjectController`
- **Models**: `Blog`, `ContactForm`, `Gallery`, `Project`
- **Repositories**: `BlogRepository`, `ContactFormRepository`, `GalleryRepository`, `ProjectRepository`
- **Services**: `BlogService`, `ContactFormService`, `GalleryService`, `ProjectService`

**Test environment** — UI tests run in jsdom. API tests use Spring Boot test slices (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`).
