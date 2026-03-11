<!-- RundownTable.vue -->
<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from "vue"

type RundownRow = Record<string, string>

const emit = defineEmits<{
  take:   [sceneId: string]
  out:    [sceneId: string]
  update: [sceneId: string, key: string, value: string]
}>()

const rows        = ref<RundownRow[]>([])
const headers     = ref<string[]>([])
const selectedIdx = ref<number | null>(null)
const fileName    = ref<string | null>(null)
const error       = ref<string | null>(null)
const tableRef    = ref<HTMLElement | null>(null)

// ─── CSV parser (no deps) ─────────────────────────────────────────────────────
function parseCsv(text: string): { headers: string[]; rows: RundownRow[] } {
  const lines = text.trim().split(/\r?\n/)
  if (lines.length < 2) return { headers: [], rows: [] }

  const parseRow = (line: string): string[] => {
    const result: string[] = []
    let cur = ""
    let inQuotes = false
    for (let i = 0; i < line.length; i++) {
      const ch = line[i]
      if (ch === '"') {
        if (inQuotes && line[i + 1] === '"') { cur += '"'; i++ }
        else inQuotes = !inQuotes
      } else if (ch === "," && !inQuotes) {
        result.push(cur.trim()); cur = ""
      } else {
        cur += ch
      }
    }
    result.push(cur.trim())
    return result
  }

  const hdrs = parseRow(lines[0] ?? "").filter(h => h !== "") // strip trailing comma artifacts
  const data = lines.slice(1).filter(l => l.trim()).map(line => {
    const vals = parseRow(line)
    return Object.fromEntries(hdrs.map((h, i) => [h, vals[i] ?? ""]))
  })

  return { headers: hdrs, rows: data }
}

// ─── A "data column" is any header containing "_" (flat key format) ───────────
// Anything without "_" (e.g. a "cue" or "label" column) is treated as row label
const dataHeaders = computed(() => headers.value.filter(h => h.includes("_")))
const labelHeader = computed(() => headers.value.find(h => !h.includes("_")) ?? null)

// ─── File loading ─────────────────────────────────────────────────────────────
function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  error.value = null
  fileName.value = file.name
  selectedIdx.value = null
  const reader = new FileReader()
  reader.onload = (ev) => {
    try {
      const { headers: hdrs, rows: data } = parseCsv(ev.target?.result as string)
      headers.value = hdrs
      rows.value = data
    } catch {
      error.value = "Failed to parse CSV."
    }
  }
  reader.readAsText(file)
}

// ─── Extract unique sceneIds from dataHeaders ─────────────────────────────────
function sceneIdsFromHeaders(): string[] {
  return [...new Set(dataHeaders.value.map(h => h.substring(0, h.lastIndexOf("_"))))]
}

// ─── Row selection → emit update per flat key ────────────────────────────────
function selectRow(idx: number) {
  selectedIdx.value = idx
  const row = rows.value[idx]

  for (const flatKey of dataHeaders.value) {
    const underscore = flatKey.lastIndexOf("_")
    const sceneId = flatKey.substring(0, underscore)
    const key     = flatKey.substring(underscore + 1)
    emit("update", sceneId, key, row?.[flatKey] ?? "")
  }

  scrollActiveRowIntoView()
}

// ─── Home: take or out each scene based on whether the row has any values ─────
function takeOrOutCurrentRow() {
  if (selectedIdx.value === null) return
  const row = rows.value[selectedIdx.value]

  for (const sceneId of sceneIdsFromHeaders()) {
    const values = dataHeaders.value
        .filter(h => h.startsWith(sceneId + "_"))
        .map(h => row?.[h] ?? "")
    const hasContent = values.some(v => v !== "")
    if (hasContent) emit("take", sceneId)
    else emit("out", sceneId)
  }
}

// ─── End: out all scenes that were used in the previous row ───────────────────
function outPrevious() {
  const prevIdx = selectedIdx.value === null ? null : selectedIdx.value
  if (prevIdx === null || prevIdx < 0) return
  const row = rows.value[prevIdx]

  for (const sceneId of sceneIdsFromHeaders()) {
    const values = dataHeaders.value
        .filter(h => h.startsWith(sceneId + "_"))
        .map(h => row?.[h] ?? "")
    const wasUsed = values.some(v => v !== "")
    if (wasUsed) emit("out", sceneId)
  }
}

// ─── Keyboard navigation ──────────────────────────────────────────────────────
function onKeyDown(e: KeyboardEvent) {
  if ((e.target as HTMLElement).tagName === "INPUT") return
  if (!rows.value.length) return

  if (e.key === "PageDown" || e.key === "ArrowDown") {
    e.preventDefault()
    const next = selectedIdx.value === null ? 0 : Math.min(selectedIdx.value + 1, rows.value.length - 1)
    selectRow(next)
  } else if (e.key === "PageUp" || e.key === "ArrowUp") {
    e.preventDefault()
    const prev = selectedIdx.value === null ? 0 : Math.max(selectedIdx.value - 1, 0)
    selectRow(prev)
  } else if (e.key === "Home" || e.key === ",") {
    e.preventDefault()
    takeOrOutCurrentRow()
  } else if (e.key === "End" || e.key === ".") {
    e.preventDefault()
    outPrevious()
  }
}

function scrollActiveRowIntoView() {
  setTimeout(() => {
    tableRef.value?.querySelector("tr.active-row")?.scrollIntoView({ block: "nearest", behavior: "smooth" })
  }, 0)
}

onMounted(()      => window.addEventListener("keydown", onKeyDown))
onBeforeUnmount(() => window.removeEventListener("keydown", onKeyDown))
</script>

<template>
  <div class="flex flex-col gap-4 font-mono">

    <!-- File picker -->
    <div class="flex items-center gap-4">
      <label class="relative cursor-pointer inline-flex items-center gap-2 px-4 py-2 rounded-md border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-xs text-zinc-300 tracking-widest uppercase transition-colors">
        <svg class="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M12 4v12m-4-4l4 4 4-4"/>
        </svg>
        {{ fileName ?? "Load Rundown CSV" }}
        <input type="file" accept=".csv" class="sr-only" @change="onFileChange" />
      </label>
      <span v-if="rows.length" class="text-xs text-zinc-600">
        {{ rows.length }} row{{ rows.length !== 1 ? "s" : "" }}
      </span>
      <!-- Keyboard hints -->
      <span v-if="rows.length" class="ml-auto text-xs text-zinc-700 flex items-center gap-3">
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">PgUp/PgDn</kbd> navigate</span>
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">Home</kbd> take/out</span>
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">End</kbd> out prev</span>
      </span>
    </div>

    <p v-if="error" class="text-xs text-red-400">{{ error }}</p>

    <!-- Empty state -->
    <div v-if="!rows.length && !error" class="flex items-center justify-center h-32 border border-dashed border-zinc-800 rounded-lg text-zinc-700 text-xs tracking-widest uppercase">
      No rundown loaded
    </div>

    <!-- Table -->
    <div v-if="rows.length" ref="tableRef" class="overflow-x-auto rounded-lg border border-zinc-800">
      <table class="w-full text-xs border-collapse">
        <thead>
        <tr class="border-b border-zinc-800 bg-zinc-900">
          <th class="px-3 py-2 text-left text-zinc-600 tracking-widest uppercase font-semibold w-6">#</th>
          <th v-if="labelHeader" class="px-3 py-2 text-left text-zinc-500 tracking-widest uppercase font-semibold">
            {{ labelHeader }}
          </th>
          <th
              v-for="h in dataHeaders" :key="h"
              class="px-3 py-2 text-left text-zinc-500 tracking-widest uppercase font-semibold whitespace-nowrap"
          >
            {{ h.replace(/_/g, " ") }}
          </th>
          <th class="px-3 py-2 w-8"></th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="(row, i) in rows"
            :key="i"
            :class="[
              'border-b border-zinc-900 cursor-pointer transition-colors',
              selectedIdx === i
                ? 'active-row bg-amber-400/10 border-l-2 border-l-amber-400'
                : 'hover:bg-zinc-800/60',
            ]"
            @click="selectRow(i)"
        >
          <td class="px-3 py-2 text-zinc-700 tabular-nums">{{ i + 1 }}</td>
          <td v-if="labelHeader" class="px-3 py-2 text-zinc-300 font-semibold whitespace-nowrap">
            {{ row[labelHeader] }}
          </td>
          <td
              v-for="h in dataHeaders" :key="h"
              class="px-3 py-2 text-zinc-400 max-w-[160px] truncate"
              :title="row[h]"
          >
            <span v-if="row[h]">{{ row[h] }}</span>
            <span v-else class="text-zinc-700">—</span>
          </td>
          <td class="px-3 py-2 text-center">
            <span v-if="selectedIdx === i" class="inline-block w-2 h-2 rounded-full bg-amber-400" />
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Status -->
    <div v-if="selectedIdx !== null" class="text-xs text-zinc-600 tracking-wide">
      Row {{ selectedIdx + 1 }} —
      <span class="text-zinc-400">PgDn</span> to advance,
      <span class="text-zinc-400">Home</span> to take/out,
      <span class="text-zinc-400">End</span> to out previous
    </div>

  </div>
</template>