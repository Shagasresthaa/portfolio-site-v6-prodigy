# API Requirements Document

Status: **draft for planning** — compiled by reading through the current state of `portfolio-site-v6-prodigy-ui` (public site) and `portfolio-site-v6-prodigy-admin` (CMS) to reverse-engineer what `portfolio-site-v6-prodigy-api` (Spring Boot 4, Java 21, PostgreSQL) needs to implement. Nothing here is built yet beyond the empty scaffold noted in each section. This doc is the input to planning, not a spec to build against verbatim — flag anything that should change before we commit to it.

Both frontends currently run entirely on mocked data: static JSON in `public/data/*.json` read through `useCachedResource` (localStorage-TTL-cached fetch), with the admin layering `localStorage` "override" blobs (upsert-by-id + deleted-ids list, or a single override blob for singletons) on top to simulate writes. **No sub-project makes a real network call anywhere today.** Everything below is what has to exist to replace that.

---

## 0. Existing scaffold (starting point)

`portfolio-site-v6-prodigy-api/src/main/java/com/sresthaa/publicui/` has empty (declaration-only) classes for:

| Model | Repository | Service | Controller | Maps to |
|---|---|---|---|---|
| `Project` | `ProjectRepository` | `ProjectService` | `ProjectController` | Projects — 1:1 name match |
| `Highlight` | `HighlightRepository` | `HighlightService` | `HighlightController` | Highlights — renamed from `Gallery` 2026-07-05 |
| `Blog` | `BlogRepository` | `BlogService` | `BlogController` | Blog posts (name is `Blog`, UI type is `BlogPost`) |
| `ContactForm` | `ContactFormRepository` | `ContactFormService` | `ContactFormController` | Contact form submissions — 1:1 |

`HeartbeatController` is the only one with real logic beyond the admin-auth entities (see §10 — `AdminAccount`/`WebAuthnCredential`/`TotpCredential`/`TotpBackupCode` are built under `com.sresthaa.admin`). `build.gradle` now includes `spring-boot-starter-security`, JJWT, webauthn4j, and a TOTP library for the admin auth work.

**Entities with no scaffold at all yet:**
- Home page content (About text + career timeline) — singleton
- Site settings / SEO — singleton
- Blog comments
- Blog like/dislike reactions
- Admin auth/session/user
- Image/media upload
- Admin 2FA, WebAuthn security keys (currently pure UI mock, real backend TBD)

---

## 1. Projects

**Public UI:** `src/types/project.ts`, `ProjectsComponent.vue`, `ProjectCard.vue`
**Admin:** `ProjectsManager.vue`, `useProjects.ts`

### Fields (`Project`)
```
id: string
name: string
shortDesc: string
longDesc?: string
statusFlag: PLANNING | IN_PROGRESS | COMPLETED | MAINTAINED | ARCHIVED
startDate: string (ISO date)
endDate?: string (ISO date)
collabMode: SOLO | GROUP
affiliation: string
affiliationType: INDEPENDENT | UNIVERSITY | ORGANIZATION | CLUB
sourceCodeAvailability: OPEN_SOURCE | CLOSED_SOURCE | UNDER_NDA
techStacks: string          # comma-separated on the wire; normalized to tech_stack table + join table server-side (see §11)
projectUrl?: string
liveUrl?: string
mediaType?: IMAGE | VIDEO   # undefined treated as IMAGE (legacy)
image?: string              # single image, no separate thumbnail
videoUrl?: string           # YouTube URL, not an uploaded file
```

### Public endpoints needed
- `GET /api/projects` — list, filtered/paginated/sorted:
  - `search` — substring match on `name` only (not description) — min 2 / max 200 chars server-side too
  - `techStacks[]` — **AND** semantics (project must contain all selected stacks, not any)
  - `page`, `pageSize` (currently fixed at 12 client-side)
  - sort fixed: `startDate DESC` (no user sort control exists yet)
  - Must also return the distinct set of all tech stacks currently in use, for filter-option population (either a dedicated endpoint or embedded in the list response's metadata)

### Admin endpoints needed
- `GET /api/admin/projects` — full list, no filter/pagination required by current UI (loads everything into one grid)
- `POST /api/admin/projects` — create
- `PUT /api/admin/projects/{id}` — update
- `DELETE /api/admin/projects/{id}` — delete
- Validation to enforce server-side (currently only client-checked): `name`/`shortDesc`/`affiliation`/`techStacks` non-empty, `startDate` required, `endDate` required when `statusFlag` is `COMPLETED`/`ARCHIVED`, cleared when `PLANNING`/`IN_PROGRESS`; media requires `image` when IMAGE, `videoUrl` when VIDEO.

---

## 2. Highlights ("Moments")

**Public UI:** `src/types/highlight.ts`, `HighlightsComponent.vue`, `HighlightCard.vue`
**Admin:** `HighlightsManager.vue`, `useHighlights.ts`

### Fields (`HighlightItem`)
```
id: string
title: string
description?: string
caption?: string
mediaType: IMAGE | VIDEO     # required (not optional, unlike Project.mediaType)
thumbnailImage?: string      # IMAGE only — low-res grid stand-in
image?: string               # IMAGE only — full-size, shown in modal
videoUrl?: string            # VIDEO only
tags: string                 # comma-separated on the wire; normalized to tag table + join table server-side (see §11)
createdAt: string (ISO)
```
Note the deliberate **two-image** split (thumbnail vs full) is a product requirement, not incidental — preserve it.

### Public endpoints needed
- `GET /api/highlights` — list:
  - `tags[]` — AND semantics
  - `page`, `pageSize` (12)
  - sort fixed: `createdAt DESC`
  - distinct tag set for filter options — trivial now (`SELECT * FROM tag`) once normalized
- Video items never use `thumbnailImage`/`image` — the YouTube embed itself is the "full" view.

### Admin endpoints needed
- `GET /api/admin/highlights`, `POST`, `PUT /{id}`, `DELETE /{id}`
- Validation: `title` and `tags` required, IMAGE requires an uploaded image, VIDEO requires `videoUrl`.

---

## 3. Blog

**Public UI:** `src/types/blog.ts`, `BlogComponent.vue`, `BlogCard.vue`, `BlogPostComponent.vue`
**Admin:** `BlogManager.vue`, `useBlogPosts.ts` (uses `md-editor-v3` + DOMPurify)

### Fields (`BlogPost`)
```
id: string
slug: string
title: string
excerpt: string
content: string        # Markdown
coverImage?: string
published: boolean
publishedAt?: string (ISO)
tags: string            # comma-separated on the wire; normalized to tag table + join table server-side (see §11)
likeCount: number
dislikeCount: number
```

### Public endpoints needed
- `GET /api/blog` — list, **server-side must force `published = true`** (never trust a client filter for this):
  - `search` — substring match on `title` only
  - `tags[]` — AND semantics
  - `page`, `pageSize` (12), sort fixed `publishedAt DESC`
- `GET /api/blog/{slug}` — detail; must 404 (not just hide) if the post is unpublished, matching current UI behavior of treating an unpublished slug as "not found."
- **Server-rendered meta tags are a hard requirement, not optional polish** — see §8.

### Admin endpoints needed
- `GET /api/admin/blog` — full list (drafts included), no filter/pagination in current UI
- `POST /api/admin/blog`, `PUT /api/admin/blog/{id}`, `DELETE /api/admin/blog/{id}`
- Slug generation is currently client-side-only (slugify button) with **no uniqueness check anywhere** — server must enforce slug uniqueness and probably should be the source of truth for validating/generating it, not just trust the client's slugify output.
- On publish: server should set `publishedAt` if not already set; on unpublish, current mock clears it — decide if we want to preserve original `publishedAt` history instead (mock behavior isn't necessarily the right production behavior here).
- Required fields (currently only client-checked): `title`, `slug`, `excerpt`, `content` non-empty.

### 3a. Blog comments (no scaffold exists)
`BlogComment` (public UI `src/types/blog.ts`):
```
id: string
name?: string       # optional; "Anonymous" shown in UI when absent — omit field entirely if anonymous, don't send literal "Anonymous"
content: string
createdAt: string (ISO)
```
- `GET /api/blog/{slug}/comments` — list, newest-first, no pagination in current mock (loads all) but should probably support pagination for production
- `POST /api/blog/{slug}/comments` — accepts `{ name?: string, content: string }`, server generates `id`/`createdAt`
  - Server-side validation needed (currently client-only): name max 100 chars, content required/non-empty/max 2000 chars
  - Rendered as plain text (auto-escaped) on the frontend — **not** parsed as HTML/Markdown, so no sanitization pipeline is needed for storage beyond standard input hygiene, but don't assume the frontend will escape a raw HTML injection attempt — validate/strip on the way in too
- No delete/moderation endpoint is implied by the *public* UI, but the admin has no comment moderation surface either — this is a gap worth deciding on explicitly (see §10 open questions) since there's currently no way to remove abusive comments at all.
- Anti-spam is completely unaddressed in the mock — needs a real decision (see §10).

### 3b. Blog like/dislike reactions (no scaffold exists)
- Current mock: `likeCount`/`dislikeCount` are static baseline numbers on the post; the current browser's own vote is tracked in `localStorage` (`blog-reaction-{slug}`) and just added visually on top — nothing is persisted server-side today, and nothing prevents duplicate votes beyond "did this browser's localStorage already record one."
- Needed:
  - `GET /api/blog/{slug}/reactions` — current aggregate counts (and, if we track identity, this browser/user's current vote state)
  - `POST /api/blog/{slug}/reactions` — cast/change/retract a vote (`{ vote: 'like' | 'dislike' | null }`), atomically adjusting counts
- **Duplicate-vote prevention needs a real design decision** — no auth/user identity exists on the public site today. Options: anonymous device ID (generated + cookied), IP-based (weak, breaks on shared/NAT'd IPs), or defer real dedup until the site has real user accounts. Flag as open question (§10) — don't silently port "one vote per browser localStorage" as the production mechanism since it provides essentially no real protection.

---

## 4. Home page content (no scaffold exists — singleton, not a collection)

**Public UI:** `HomeComponent.vue` consuming `home.json` via `useCachedResource`
**Admin:** `HomeDashboard.vue`, `useHomeContent.ts` — reorderable lists via move-up/down buttons, "Reset to defaults" support

### Shape (`HomeContent`)
```
aboutHook: string
aboutStory: string[]          # ordered paragraphs
timeline: TimelineEvent[]     # ordered career events
```
`TimelineEvent` (`src/types/timeline.ts`):
```
title: string
position: string
institution: string
date: string        # free text, e.g. "2023" — not a real date type
duration: string     # free text, e.g. "6 months"
```

### Endpoints needed
- `GET /api/home` — public read
- `GET /api/admin/home`, `PUT /api/admin/home` — singleton read/write (whole-object replace, matching the admin's "one override blob wins wholesale" mental model), including array order for `aboutStory`/`timeline` since reordering is a real feature
- No validation exists in the admin mock at all today (`handleSave` saves unconditionally) — decide if the API should add any (e.g. non-empty hook) or intentionally stay permissive.

---

## 5. Site settings / SEO (no scaffold exists — singleton)

**Public UI:** `stores/siteSettings.ts`, `useDocumentMeta.ts`, `router/index.ts`'s `applyBaselineMeta`
**Admin:** `SiteSettingsComponent.vue`, `useSiteSettings.ts`

### Fields (`SiteSettings`)
```
siteTitle: string
defaultDescription: string
defaultShareImage?: string
searchIndexingEnabled: boolean
```
Acts as the fallback for every page **except** blog posts (which use their own `title`/`excerpt`/`coverImage` directly instead of separate SEO overrides — a deliberate, already-made product decision, not an oversight to "fix").

### Endpoints needed
- `GET /api/site-settings` — public read
- `GET /api/admin/site-settings`, `PUT /api/admin/site-settings` — singleton read/write
- No length caps or extra validation exist in the admin mock beyond HTML `required` on two fields — worth adding real length limits server-side since these values land directly in `<meta>` tags.
- **This entity is entangled with §8 (server-rendered meta) — it's not just CRUD.** The values need to be readable at HTML-render time by whatever serves the public site's initial response, not just fetched client-side after load.

---

## 6. Contact

**Public UI (submitter side):** `ContactComponent.vue` — currently `console.log`s the payload and fakes success unconditionally, no network call
**Admin (reader side):** `ContactMessagesComponent.vue`, `useContactMessages.ts` — read/delete only, no message-content editing

### Submission payload
```
name?: string       # omitted if "post anonymously" is checked
email: string        # required
subject?: string
message: string      # required
```
Client-side validation to replicate/enforce server-side: name max 100, email max 254 + format check, subject max 150, message max 5000.

### Stored shape (admin side, `ContactMessage`)
```
id: string
name: string | null
email: string
subject: string | null
message: string
read: boolean
createdAt: string (ISO)
```

### Endpoints needed
- `POST /api/contact` — public submit; **must return a real success/failure signal** — today the UI fakes success unconditionally with no error path at all, which needs to change once a real endpoint can actually fail.
- `GET /api/admin/contact` — list (no pagination/search in current UI, but consider it for production since inboxes grow unboundedly)
- `PATCH /api/admin/contact/{id}` or similar — mark as read
- `DELETE /api/admin/contact/{id}` — delete
- **Anti-spam is completely absent from the mock** (no CAPTCHA, honeypot, or rate-limiting of any kind) — flag as open question (§10), since a real public endpoint with none of this is wide open to bots.

---

## 7. Naming reconciliation

| Scaffold name | UI-facing name | Status |
|---|---|---|
| ~~`Gallery`~~ → `Highlight` | "Highlights" | **Renamed 2026-07-05** |
| `Blog` | `BlogPost` (UI type name) | Kept as `Blog` — comments/reactions still need their own new entities regardless |

---

## 8. Server-side rendering / meta-tag requirement (cross-cutting, not just CRUD)

**Scope for initial launch (2026-07-05): link previews only, not search-engine indexing.** The immediate goal is that sharing a `/blog/:slug` URL on LinkedIn/Discord/iMessage/etc. shows a real title/description/image instead of a bare link — that only requires server-rendered `<meta>` tags (below), not the actual article content being crawlable. Getting individual posts indexed/ranked on search engines (Medium-style) is a **separate, larger, explicitly deferred** piece of work — see the "Deferred: real search-engine indexing" subsection at the end of this section. Priority right now is shipping and replacing the old site, not search SEO.

This is the one requirement that isn't a normal REST endpoint and is easy to under-scope if treated as "just add a GET":

- Messaging apps and most crawlers (iMessage, Discord, WhatsApp, Slack, Bing) fetch **raw server HTML and do not execute JavaScript** — the current client-side `document.head` mutation (`useDocumentMeta.ts`) is invisible to them. Only Google's crawler executes JS at all, and only in a delayed second pass.
- **`/blog/:slug` needs per-post server-rendered `<title>`/`og:*`/`twitter:*` tags**, sourced from that post's `title`/`excerpt`/`coverImage`, present in the *initial* HTML response.
- **Every other route** (Home/Projects/Highlights/Contact) needs the site-wide `SiteSettings` baseline tags server-rendered similarly, since none of them have per-entity content of their own.
- `searchIndexingEnabled` must produce a real `<meta name="robots">` in the server response, not just a client-side injection, for non-JS crawlers to respect it.
- A **dev-only proof of this mechanism already exists**: `portfolio-site-v6-prodigy-ui/vite.config.ts`'s `blogPostMetaDevPlugin` (`apply: 'serve'` only) intercepts `/blog/:slug` requests in Vite's dev server and injects the same tags by reading `blog.json` directly, using the request's `Host` header for absolute URLs. It has **zero effect in production** (no per-request server logic exists in a static build) — Spring Boot needs an equivalent (templated shell per route/slug, or crawler-UA-detected prerendering).
### Deferred: real search-engine indexing (later phase, not this launch)

Meta tags alone don't get individual posts indexed/ranked — that needs a crawler to see the actual **article content**, not just its title/description. Today the article body only exists after the Vue SPA runs client-side; Google's crawler eventually executes JS (delayed second pass) so it may index over time regardless, but Bing/DuckDuckGo/etc. largely don't execute JS at all and would see nothing. Since blog content is markdown already rendered client-side (`utils/markdown.ts`), the eventual fix isn't adopting a full SSR framework (Nuxt etc.) — it's having `BlogController` render that same markdown to real HTML server-side for the initial `/blog/:slug` response, with the Vue app still mounting on top for interactive parts (comments/reactions/share). Bundled with that, when this phase happens:
- `sitemap.xml` listing every published slug
- `<link rel="canonical">` per post
- JSON-LD `Article` structured data (headline, datePublished, author)
- `searchIndexingEnabled` robots directive wired server-side too, not just client-injected

**Explicitly not part of the initial API build** — revisit once the site is live and stable on the new stack.

---

## 9. Media / image upload — storage backend **DECIDED** (Cloudflare R2)

**Every image today is base64-embedded, client-side only — there is no upload endpoint anywhere.**

**Decided (2026-07-05, confirmed from a prior conversation):** uploaded images will be stored in **Cloudflare R2**, not local disk on the Spring Boot server — chosen to avoid managing disk storage/backups on the app server and for R2's no-egress-fee pricing, which suits a low-traffic personal site. See memory: `project_api_image_storage`. Still to work out at implementation time: presigned-upload-URL flow (client uploads directly to R2) vs a server-side proxy-to-R2 endpoint, and whether resize/re-encode happens before or after landing in R2.

- Admin's `useImageProcessing.ts` resizes/re-encodes to WebP client-side (1600px/q0.82 full, 480px/q0.75 thumbnail where applicable) and returns a `data:image/webp;base64,...` string, which then gets stored **directly as the field value** (`coverImage`, `image`, `thumbnailImage`, `defaultShareImage`) inside the `localStorage` override blob.
- This is explicitly called out in the admin source as a stopgap ("swap for a real upload endpoint... once one exists") and is **already hitting practical limits** — large base64 blobs inside `localStorage` JSON risk the ~5–10MB per-origin quota with more than a handful of posts/moments/projects.
- **API implication:** `image`/`coverImage`/`thumbnailImage`/`defaultShareImage` fields should become **URLs pointing at server-stored/served assets**, not inline data. Needs a real upload endpoint (multipart form upload, or presigned-URL flow) — ideally doing the same resize/re-encode server-side (or better) rather than trusting the client's pre-resized output.
- Video is never actually uploaded anywhere (Highlights/Projects VIDEO mode only ever takes a YouTube URL) — no video file upload path needs to be designed.
- No cropping UI exists — straight aspect-preserving downscale only; fine to keep that assumption unless product wants cropping later.
- `techStacks`/`tags` are currently comma-separated free-text strings with no shared tag/tech-stack table or admin autocomplete — being normalized into real `tech_stack`/`tag` tables instead, see §11.

---

## 10. Auth / account management — **DECIDED**

**Admin (`stores/auth.ts`, `AuthGate.vue`, and the `/account/*` pages) has no real backend today** — a single hardcoded credential pair from env vars, a fake bearer token, 24h session in `localStorage`. The mock's own comments explicitly describe this as "the shape it'll need once this is swapped for a real `fetch('/api/admin/auth/login', ...)` call" — i.e. it was built anticipating this doc.

**Decided approach (2026-07-05):** WebAuthn (security keys — YubiKey, and passkeys generally) as the primary second factor, combined with short-lived, issued-time-constrained JWTs for session auth. TOTP-based mobile authenticator (real QR provisioning) is being built too, as a backup factor in case the physical security key is lost — both 2FA paths are in scope, not just one. No multi-user/roles system — single implicit admin, matching current scope. See memory: `project_api_auth_decision`.

Needed:
- `POST /api/admin/auth/login` — real credential check → issues a short-lived JWT, replacing the mock bearer token
- A real user/credential store — hashed password, supporting the existing password-change/username-change flows against it (current mock stores the new password in **plaintext** in `localStorage`, which must not carry over)
- Real WebAuthn relying-party registration (`navigator.credentials.create()`) and assertion (`navigator.credentials.get()`) endpoints — replaces the current named-placeholder-only security-keys list
- Real TOTP enrollment (secret generation, QR provisioning URI) + verification against that secret, and real backup-code redemption tracking — replaces the current mock that accepts any 6-digit code

---

## 11. Decisions log + remaining open question

Resolved 2026-07-05:
- **Auth**: WebAuthn (security keys) + TOTP backup + short-lived JWTs. See §10.
- **Image storage**: Cloudflare R2. See §9.
- **Duplicate-vote prevention for blog reactions**: deliberately **not prioritized**. This site has no visitor accounts by design (see memory: `project_no_user_accounts_philosophy`) — adding real identity just to dedup likes/dislikes would reintroduce the exact account-security burden that was already removed a few iterations ago. Weak/no dedup is an accepted tradeoff.
- **Comment moderation**: no admin moderation UI planned — will be handled manually via direct DB edits if/when needed.
- **Anti-spam for Contact form + comments**: acknowledged real gap, explicitly deferred until after core CRUD/API work is done. Not a blocker for core work.
- **2FA / WebAuthn scope**: both are in scope for real (not cut) — see §10.

Resolved 2026-07-05 (cont'd):
- **Server-rendered meta tags / SEO (§8)**: scoped down to link previews only for initial launch (server-rendered `og:`/`twitter:` tags per blog post + site-wide baseline). Real search-engine indexing (server-rendered article body, sitemap.xml, canonical tags, JSON-LD) is explicitly deferred to a later phase — priority is shipping and replacing the old stale/vulnerable site, not organic search SEO right now.
- **`Gallery` naming**: renamed to `Highlight` (model/repository/service/controller). See §7.
- **`techStacks`/`tags` shape — REVISED, now normalized**: two lookup tables (`tech_stack`, `tag`, case-insensitively unique) plus three join tables (`project_tech_stack`, `highlight_tag`, `blog_tag`) — `@ManyToMany` from `Project`/`Highlight`/`Blog`. Decided over the array-column option specifically because the schema is still greenfield (no real rows yet in any of these three entities), making this the cheapest point to normalize — doing it later would mean a data migration on top of the schema change. Admin UI will need a real tag-picker/autocomplete component (querying existing tags) instead of the current plain text input, and service-layer writes need find-or-create-by-name-case-insensitive logic when linking tags to an entity.
- **Contact message read state**: confirmed a per-message read/unread boolean is sufficient, no richer model needed.

No open decisions remain from the original list — DB migration tooling (Flyway/etc.) was also considered and deliberately skipped, see `application-local.properties` and memory `feedback_risk_proportional_tooling`.
