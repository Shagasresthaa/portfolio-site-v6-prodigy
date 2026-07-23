#!/usr/bin/env node
// Post-build: replaces Layout.astro's __INLINE_SCRIPT_HASHES__ CSP placeholder with each page's
// real inline-script sha256 hashes, so Astro version bumps can't leave stale hashes behind.
import { readdirSync, readFileSync, writeFileSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';

const DIST_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist');
const PLACEHOLDER = '__INLINE_SCRIPT_HASHES__';
const INLINE_SCRIPT_RE = /<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g;

function walkHtmlFiles(dir) {
  const out = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) out.push(...walkHtmlFiles(full));
    else if (entry.endsWith('.html')) out.push(full);
  }
  return out;
}

function inlineScriptHashesFor(html) {
  const hashes = new Set();
  for (const match of html.matchAll(INLINE_SCRIPT_RE)) {
    const hash = createHash('sha256').update(match[1], 'utf8').digest('base64');
    hashes.add(`'sha256-${hash}'`);
  }
  return [...hashes];
}

const files = walkHtmlFiles(DIST_DIR);
let patched = 0;
for (const file of files) {
  const html = readFileSync(file, 'utf8');
  if (!html.includes(PLACEHOLDER)) continue;
  writeFileSync(file, html.replace(PLACEHOLDER, inlineScriptHashesFor(html).join(' ')));
  patched++;
}

if (patched === 0) {
  console.error(`fix-csp-hashes: no dist/**/*.html contained ${PLACEHOLDER} - did Layout.astro's CSP change shape?`);
  process.exit(1);
}
console.log(`fix-csp-hashes: patched ${patched} file(s) with fresh inline-script CSP hashes`);
