import type { Project } from '@/types/project'

const OVERRIDES_KEY = 'admin-projects-overrides'
const BASE_URL = '/data/projects.json'

interface Overrides {
  // Both edits to existing projects and brand-new ones live here, keyed by
  // id - there's no separate "create" endpoint to call yet (see CLAUDE.md:
  // ProjectController is scaffolded but empty), so new projects are just
  // upserts whose id doesn't match anything in the base JSON.
  upserts: Record<string, Project>
  deletedIds: string[]
}

function readOverrides(): Overrides {
  const raw = localStorage.getItem(OVERRIDES_KEY)
  if (!raw) return { upserts: {}, deletedIds: [] }
  try {
    const parsed = JSON.parse(raw) as Partial<Overrides>
    return { upserts: parsed.upserts ?? {}, deletedIds: parsed.deletedIds ?? [] }
  } catch {
    return { upserts: {}, deletedIds: [] }
  }
}

function writeOverrides(overrides: Overrides) {
  localStorage.setItem(OVERRIDES_KEY, JSON.stringify(overrides))
}

async function fetchBaseProjects(): Promise<Project[]> {
  const response = await fetch(BASE_URL)
  if (!response.ok) throw new Error(`Failed to fetch ${BASE_URL}: ${response.status}`)
  const data = (await response.json()) as { projects: Project[] }
  return data.projects
}

/** Base projects from projects.json with local create/edit/delete overrides layered on top. */
export async function loadProjects(): Promise<Project[]> {
  const base = await fetchBaseProjects()
  const { upserts, deletedIds } = readOverrides()
  const deleted = new Set(deletedIds)

  const existing = base
    .filter((project) => !deleted.has(project.id))
    .map((project) => upserts[project.id] ?? project)
  const created = Object.values(upserts).filter(
    (project) => !base.some((baseProject) => baseProject.id === project.id) && !deleted.has(project.id),
  )

  return [...created, ...existing].sort(
    (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime(),
  )
}

/** Throws if the browser's storage quota is exceeded (large images add up fast). */
export function upsertProject(project: Project) {
  const overrides = readOverrides()
  overrides.upserts[project.id] = project
  writeOverrides(overrides)
}

export function deleteProject(id: string) {
  const overrides = readOverrides()
  delete overrides.upserts[id]
  overrides.deletedIds = [...new Set([...overrides.deletedIds, id])]
  writeOverrides(overrides)
}
