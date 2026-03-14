<script setup lang="ts">
import { ref, nextTick, toRaw, computed } from "vue"
import type { Row } from "../useRundown.ts"
import { formatTime } from "../useTimeFormat.ts"
import { useReorder } from "../useReorder.ts"

console.log("geg")

const props = defineProps<{
  rows:      Row[]
  readonly?: boolean
}>()

const emit = defineEmits<{
  addRow:      [row: Row]
  updateRow:   [row: Row]
  reorderRows: [rows: Row[]]
  selectRow:   [row: Row]
  captureRow:  [row: Row]
}>()

// ── Row reorder ───────────────────────────────────────────────────────────────
const { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd } =
    useReorder(() => props.rows, (rows) => emit("reorderRows", rows))

// ── Add row ───────────────────────────────────────────────────────────────────
const adding  = ref(false)
const firstEl = ref<HTMLInputElement | null>(null)
const draft   = ref({ name: "", startTime: "", duration: "", notes: "" })

async function startAdd() {
  adding.value = true
  draft.value  = { name: "", startTime: "", duration: "", notes: "" }
  await nextTick()
  firstEl.value?.focus()
}

function parseTime(raw: string): number {
  const parts = raw.trim().split(":").map(Number)
  if (parts.some(isNaN)) return 0
  if (parts.length === 3) return (((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 60 + (parts[2] ?? 0)) * 1000
  if (parts.length === 2) return ((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 1000
  return (parts[0] ?? 0) * 1000
}

function formatTimeForInput(ms: number): string {
  if (!ms) return ""
  const totalSecs = Math.floor(ms / 1000)
  const h = Math.floor(totalSecs / 3600)
  const m = Math.floor((totalSecs % 3600) / 60)
  const s = totalSecs % 60
  if (h > 0) return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
  return `${m}:${String(s).padStart(2, "0")}`
}

function commitAdd() {
  const name = draft.value.name.trim()
  adding.value = false
  emit("addRow", {
    id:        crypto.randomUUID(),
    name,
    startTime: parseTime(draft.value.startTime),
    duration:  parseTime(draft.value.duration),
    notes:     draft.value.notes.trim(),
    values:    [],
  })
}

// ── Uncontrolled inline editing ───────────────────────────────────────────────
type EditingField = "name" | "startTime" | "duration" | "notes"

function seedValue(row: Row, field: EditingField): string {
  if (field === "name")      return row.name
  if (field === "startTime") return formatTimeForInput(row.startTime)
  if (field === "duration")  return formatTimeForInput(row.duration)
  return row.notes
}

function onFocus(row: Row, field: EditingField, el: HTMLInputElement) {
  el.value = seedValue(row, field)
}

function onBlur(row: Row, field: EditingField, el: HTMLInputElement) {
  const value = el.value

  if (field === "duration") el.value = formatTime(row.duration)

  const raw     = toRaw(row)
  const updated: Row = { ...raw, values: raw.values.map(v => ({ ...v })) }
  let changed = false

  if (field === "name") {
    const trimmed = value.trim()
    if (!trimmed || trimmed === row.name) return
    updated.name = trimmed
    changed = true
  } else if (field === "startTime") {
    const parsed = parseTime(value)
    if (parsed === row.startTime) return
    updated.startTime = parsed
    changed = true
  } else if (field === "duration") {
    const parsed = parseTime(value)
    if (parsed === row.duration) return
    updated.duration = parsed
    changed = true
  } else if (field === "notes") {
    const trimmed = value.trim()
    if (trimmed === row.notes) return
    updated.notes = trimmed
    changed = true
  }

  if (changed) emit("updateRow", updated)
}

function onKeydown(e: KeyboardEvent, row: Row, field: EditingField, el: HTMLInputElement) {
  if (e.key === "Enter") {
    e.preventDefault()
    el.blur()
  } else if (e.key === "Escape") {
    e.preventDefault()
    if (field === "duration") el.value = formatTime(row.duration)
    else el.value = seedValue(row, field)
    el.blur()
  }
}

// ── Values summary ────────────────────────────────────────────────────────────
// Must be a computed (not a plain function) so Vue re-evaluates per-row when
// values change. A plain function called in v-for evaluates once and its result
// leaks to every subsequent row — only the first row's summary would show.
const valueSummaries = computed(() => {
  const map: Record<string, string> = {}
  for (const row of props.rows) {
    if (!row.values.length) { map[row.id] = "—"; continue }
    const activeScenes = new Set<string>()
    for (const p of row.values) {
      if (!p.value) continue
      const scene = p.name.includes(".") ? p.name.split(".")[0] : p.name
      if (scene) activeScenes.add(scene)
    }
    map[row.id] = activeScenes.size ? [...activeScenes].join("  ·  ") : "—"
  }
  return map
})
</script>

<template>
  <div class="w-full" @dragover.stop.prevent @drop.stop.prevent @dragend.stop>

    <!-- Empty state -->
    <div
        v-if="rows.length === 0 && !adding"
        class="pl-6 pr-6 py-8 text-sm text-zinc-600 italic"
    >
      No rows on this page
    </div>

    <table v-else class="w-full table-fixed text-sm border-collapse">
      <colgroup>
        <col class="w-8" />
        <col class="min-w-23" />
        <col class="min-w-30 w-[20%]" />
        <col class="min-w-20" />
        <col class="min-w-20" />
        <col class="w-[20%]" />
        <col />
        <col class="w-23" />
      </colgroup>

      <thead>
      <tr class="border-b border-zinc-800">
        <th></th>
        <th></th>
        <th class="pl-3 pr-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Name</th>
        <th class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500 tabular-nums">Start</th>
        <th class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500 tabular-nums">Duration</th>
        <th class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Notes</th>
        <th class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Values</th>
        <th class="pr-4 py-3" />
      </tr>
      </thead>

      <tbody>
      <tr
          v-for="row in rows"
          :key="row.id"
          class="group border-b border-zinc-800/50 transition-colors"
          :class="{
            'hover:bg-zinc-800/40': !readonly,
            'opacity-30': draggingId === row.id,
          }"
          @dragend="onDragEnd($event)"
      >

        <td
            class="relative pl-2"
            @dragover="onDragOver($event, row.id)"
            @drop="onDrop($event, row.id)"
        >
          <div
              class="absolute top-0 left-0 right-0 h-0.5 bg-blue-500 transition-opacity duration-100 pointer-events-none"
              :class="dragOverId === row.id ? 'opacity-100' : 'opacity-0'"
          />
          <div
              v-if="!readonly"
              draggable="true"
              class="flex items-center justify-center cursor-grab active:cursor-grabbing text-zinc-700 hover:text-zinc-400 transition-colors px-1 py-2"
              title="Drag to reorder row"
              @dragstart="onDragStart($event, row.id)"
          >
            <svg viewBox="0 0 10 16" width="8" height="14" fill="currentColor">
              <circle cx="3" cy="3"  r="1.2"/><circle cx="7" cy="3"  r="1.2"/>
              <circle cx="3" cy="8"  r="1.2"/><circle cx="7" cy="8"  r="1.2"/>
              <circle cx="3" cy="13" r="1.2"/><circle cx="7" cy="13" r="1.2"/>
            </svg>
          </div>
        </td>

        <!-- Select -->
        <td>
          <div class="flex w-full items-center justify-center">
            <button
                class="px-4 py-1 rounded bg-primary-muted/25 hover:bg-primary hover:text-text text-primary transition-color duration-125 cursor-pointer text-sm font-medium"
                title="Send this row's values to scenes"
                @click.stop="emit('selectRow', row)"
            >Select</button>
          </div>
        </td>

        <!-- Name -->
        <td class="pl-6 pr-3 py-2">
          <input
              :value="row.name"
              :readonly="readonly"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm font-medium transition-colors"
              :class="readonly
                  ? 'text-zinc-500 cursor-default select-none pointer-events-none'
                  : 'text-zinc-100 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row, 'name', $event.target as HTMLInputElement)"
              @blur="onBlur(row, 'name', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row, 'name', $event.target as HTMLInputElement)"
          />
        </td>

        <!-- Start -->
        <td class="px-3 py-2 tabular-nums whitespace-nowrap">
          <input
              :value="formatTimeForInput(row.startTime)"
              :readonly="readonly"
              placeholder="0:00"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm tabular-nums transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-400 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row, 'startTime', $event.target as HTMLInputElement)"
              @blur="onBlur(row, 'startTime', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row, 'startTime', $event.target as HTMLInputElement)"
          />
        </td>

        <!-- Duration -->
        <td class="px-3 py-2 tabular-nums whitespace-nowrap">
          <input
              :value="formatTime(row.duration)"
              :readonly="readonly"
              placeholder="0:00"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm tabular-nums transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-400 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row, 'duration', $event.target as HTMLInputElement)"
              @blur="onBlur(row, 'duration', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row, 'duration', $event.target as HTMLInputElement)"
          />
        </td>

        <!-- Notes -->
        <td class="px-3 py-2">
          <input
              :value="row.notes"
              :readonly="readonly"
              placeholder="—"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-500 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row, 'notes', $event.target as HTMLInputElement)"
              @blur="onBlur(row, 'notes', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row, 'notes', $event.target as HTMLInputElement)"
          />
        </td>

        <!-- Values summary -->
        <td class="px-3 py-2">
          <span class="block truncate text-zinc-600 text-xs font-mono px-2 py-1.5">
            {{ valueSummaries[row.id] }}
          </span>
        </td>

        <!-- Actions -->
        <td class="py-2">
          <div
              v-if="!readonly"
              class="flex w-full items-center justify-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <button
                class="px-4 py-1 rounded bg-amber-900/25 hover:bg-amber-500/50 hover:text-text text-amber-500/75 transition-color duration-125 cursor-pointer text-sm font-medium"
                title="Capture current scene values into this row"
                @click.stop="emit('captureRow', row)"
            >Update</button>
          </div>
        </td>
      </tr>

      <!-- Add row form -->
      <tr v-if="adding && !readonly" class="bg-zinc-800/20 border-t border-zinc-700/50">
        <td></td>
        <td></td>
        <td class="pl-6 pr-3 py-3">
          <input
              ref="firstEl"
              v-model="draft.name"
              placeholder="Row name"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-3 py-3">
          <input
              v-model="draft.startTime"
              placeholder="00:00"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-3 py-3">
          <input
              v-model="draft.duration"
              placeholder="00:00"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-3 py-3" colspan="3">
          <div class="flex items-center gap-2">
            <input
                v-model="draft.notes"
                placeholder="Notes"
                class="flex-1 bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600"
                @keydown.enter="commitAdd"
                @keydown.esc="adding = false"
            />
            <button
                class="text-sm px-4 py-2 rounded-md bg-zinc-700 hover:bg-zinc-600 text-zinc-200 hover:text-white transition-colors shrink-0 font-medium"
                @click="commitAdd"
            >Add</button>
            <button
                class="text-sm text-zinc-600 hover:text-zinc-300 transition-colors shrink-0 px-1"
                @click="adding = false"
            >✕</button>
          </div>
        </td>
      </tr>
      </tbody>
    </table>

    <!-- Add row button -->
    <div v-if="!readonly" class="pl-6 pr-6 py-4 border-t border-zinc-800/50">
      <button
          class="flex items-center gap-2 text-sm text-zinc-500 hover:text-zinc-200 transition-colors py-1"
          @click="startAdd"
      >
        <svg viewBox="0 0 12 12" width="11" height="11" fill="none">
          <path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        Add row
      </button>
    </div>

  </div>
</template>