# Portfolio Site v6 Prodigy

Personal portfolio website — version 6. Built as a monorepo with a Vue 3 frontend and a Spring Boot REST API backed by PostgreSQL.

## Repository Structure

```
portfolio-site-v6-prodigy/
├── portfolio-site-v6-prodigy-ui/   # Vue 3 frontend
└── portfolio-site-v6-prodigy-api/  # Spring Boot API
```

---

## UI (`portfolio-site-v6-prodigy-ui`)

**Stack:** Vue 3 · TypeScript · Vite · Pinia · Vue Router · Vitest

### Setup

Requires Node `^22.18.0 || >=24.12.0` and [pnpm](https://pnpm.io/).

```bash
cd portfolio-site-v6-prodigy-ui
pnpm install
```

### Development

```bash
pnpm dev          # start dev server with HMR
pnpm build        # type-check + production build
pnpm test:unit    # run unit tests
pnpm lint         # lint and auto-fix
pnpm format       # format source files
```

---

## API (`portfolio-site-v6-prodigy-api`)

**Stack:** Java 21 · Spring Boot 4 · Spring MVC · Spring Data JPA · PostgreSQL

### Setup

Requires Java 21 and a running PostgreSQL instance. Configure your local database connection before running.

### Development

```bash
cd portfolio-site-v6-prodigy-api
./gradlew bootRun   # start the API server
./gradlew test      # run tests
./gradlew build     # compile, test, and package
```
