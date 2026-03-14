<script setup lang="ts">
import { ref, nextTick, computed } from "vue"
import type { Row } from "../useRundown.ts"
import { formatTime, parseTimeInput } from "../useTimeFormat.ts"

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
function moveUp(index: number) {
  if (index === 0) return
  const items = [...props.rows]
  const [item] = items.splice(index, 1)
  if (!item) return
  items.splice(index - 1, 0, item)
  emit("reorderRows", items)
}

function moveDown(index: number) {
  if (index >= props.rows.length - 1) return
  const items = [...props.rows]
  const [item] = items.splice(index, 1)
  if (!item) return
  items.splice(index + 1, 0, item)
  emit("reorderRows", items)
}

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

function commitAdd() {
  const name = draft.value.name.trim()
  adding.value = false
  emit("addRow", {
    id:        crypto.randomUUID(),
    name,
    startTime: parseTimeInput(draft.value.startTime),
    duration:  parseTimeInput(draft.value.duration),
    notes:     draft.value.notes.trim(),
    values:    [],
  })
}

// ── Inline editing ────────────────────────────────────────────────────────────
type EditingField = "name" | "startTime" | "duration" | "notes"

function displayValue(row: Row, field: EditingField): string {
  if (field === "name")      return row.name
  if (field === "startTime") return formatTime(row.startTime)
  if (field === "duration")  return formatTime(row.duration)
  return row.notes
}

function onFocus(_rowId: string, _field: EditingField, _el: HTMLInputElement) {
  // Keep the current formatted value — user edits in-place
}

function onBlur(rowId: string, field: EditingField, el: HTMLInputElement) {
  const value = el.value

  // Find the current row by ID from props to avoid stale reactive refs
  const current = props.rows.find(r => r.id === rowId)
  if (!current) return

  // Build a plain updated copy
  const updated: Row = {
    id:        current.id,
    name:      current.name,
    startTime: current.startTime,
    duration:  current.duration,
    notes:     current.notes,
    values:    current.values.map(v => ({ ...v })),
  }
  let changed = false

  if (field === "name") {
    const trimmed = value.trim()
    if (!trimmed || trimmed === current.name) { el.value = current.name; return }
    updated.name = trimmed
    changed = true
  } else if (field === "startTime") {
    const parsed = parseTimeInput(value)
    el.value = formatTime(parsed)
    if (parsed === current.startTime) return
    updated.startTime = parsed
    changed = true
  } else if (field === "duration") {
    const parsed = parseTimeInput(value)
    el.value = formatTime(parsed)
    if (parsed === current.duration) return
    updated.duration = parsed
    changed = true
  } else if (field === "notes") {
    const trimmed = value.trim()
    if (trimmed === current.notes) return
    updated.notes = trimmed
    changed = true
  }

  if (changed) emit("updateRow", updated)
}

function onKeydown(e: KeyboardEvent, rowId: string, field: EditingField, el: HTMLInputElement) {
  if (e.key === "Enter") {
    e.preventDefault()
    el.blur()
  } else if (e.key === "Escape") {
    e.preventDefault()
    const current = props.rows.find(r => r.id === rowId)
    if (current) el.value = displayValue(current, field)
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
  <div class="w-full">

    <!-- Empty state -->
    <div
        v-if="rows.length === 0 && !adding"
        class="pl-6 pr-6 py-8 text-sm text-zinc-600 italic"
    >
      No rows on this page
    </div>

    <div v-else class="text-sm">

      <!-- Header -->
      <div class="grid grid-cols-[2.5rem_5.75rem_1fr_5rem_5rem_1fr_1fr_5.75rem] border-b border-zinc-800">
        <div></div>
        <div></div>
        <div class="pl-3 pr-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Name</div>
        <div class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500 tabular-nums">Start</div>
        <div class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500 tabular-nums">Duration</div>
        <div class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Notes</div>
        <div class="px-3 py-3 text-left text-xs font-semibold tracking-widest uppercase text-zinc-500">Values</div>
        <div class="pr-4 py-3"></div>
      </div>

      <!-- Rows -->
      <div
          v-for="(row, index) in rows"
          :key="row.id"
          class="group relative grid grid-cols-[2.5rem_5.75rem_1fr_5rem_5rem_1fr_1fr_5.75rem] items-center border-b border-zinc-800/50 transition-colors"
          :class="{ 'hover:bg-zinc-800/40': !readonly }"
      >

        <!-- Move up / down -->
        <div v-if="!readonly" class="flex flex-col items-center justify-center">
          <button
              class="p-0.5 rounded text-zinc-700 hover:text-zinc-300 hover:bg-zinc-700 transition-colors disabled:opacity-20 disabled:pointer-events-none"
              :disabled="index === 0"
              title="Move up"
              @click.stop="moveUp(index)"
          >
            <svg viewBox="0 0 12 12" width="12" height="12" fill="none">
              <path d="M2 8l4-4 4 4" stroke="currentColor" stroke-width="1.5"
                    stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <button
              class="p-0.5 rounded text-zinc-700 hover:text-zinc-300 hover:bg-zinc-700 transition-colors disabled:opacity-20 disabled:pointer-events-none"
              :disabled="index === rows.length - 1"
              title="Move down"
              @click.stop="moveDown(index)"
          >
            <svg viewBox="0 0 12 12" width="12" height="12" fill="none">
              <path d="M2 4l4 4 4-4" stroke="currentColor" stroke-width="1.5"
                    stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <div v-else></div>

        <!-- Select -->
        <div class="flex items-center justify-center">
          <button
              class="px-4 py-1 rounded bg-primary-muted/25 hover:bg-primary hover:text-text text-primary transition-color duration-125 cursor-pointer text-sm font-medium"
              title="Send this row's values to scenes"
              @click.stop="emit('selectRow', row)"
          >Select</button>
        </div>

        <!-- Name -->
        <div class="pl-6 pr-3 py-2">
          <input
              :value="row.name"
              :readonly="readonly"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm font-medium transition-colors"
              :class="readonly
                  ? 'text-zinc-500 cursor-default select-none pointer-events-none'
                  : 'text-zinc-100 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row.id, 'name', $event.target as HTMLInputElement)"
              @blur="onBlur(row.id, 'name', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row.id, 'name', $event.target as HTMLInputElement)"
          />
        </div>

        <!-- Start -->
        <div class="px-3 py-2 tabular-nums whitespace-nowrap">
          <input
              :value="formatTime(row.startTime)"
              :readonly="readonly"
              placeholder="00:00"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm tabular-nums transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-400 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row.id, 'startTime', $event.target as HTMLInputElement)"
              @blur="onBlur(row.id, 'startTime', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row.id, 'startTime', $event.target as HTMLInputElement)"
          />
        </div>

        <!-- Duration -->
        <div class="px-3 py-2 tabular-nums whitespace-nowrap">
          <input
              :value="formatTime(row.duration)"
              :readonly="readonly"
              placeholder="00:00"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm tabular-nums transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-400 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row.id, 'duration', $event.target as HTMLInputElement)"
              @blur="onBlur(row.id, 'duration', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row.id, 'duration', $event.target as HTMLInputElement)"
          />
        </div>

        <!-- Notes -->
        <div class="px-3 py-2">
          <input
              :value="row.notes"
              :readonly="readonly"
              placeholder="—"
              class="w-full bg-transparent px-2 py-1.5 rounded text-sm transition-colors"
              :class="readonly
                  ? 'text-zinc-600 cursor-default select-none pointer-events-none'
                  : 'text-zinc-500 cursor-text hover:bg-zinc-800/60 focus:bg-zinc-800 focus:outline-none focus:ring-1 focus:ring-zinc-600'"
              @focus="onFocus(row.id, 'notes', $event.target as HTMLInputElement)"
              @blur="onBlur(row.id, 'notes', $event.target as HTMLInputElement)"
              @keydown="onKeydown($event, row.id, 'notes', $event.target as HTMLInputElement)"
          />
        </div>

        <!-- Values summary -->
        <div class="px-3 py-2">
          <span class="block truncate text-zinc-600 text-xs font-mono px-2 py-1.5">
            {{ valueSummaries[row.id] }}
          </span>
        </div>

        <!-- Actions -->
        <div class="py-2">
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
        </div>
      </div>

      <!-- Add row form -->
      <div v-if="adding && !readonly"
           class="grid grid-cols-[2.5rem_5.75rem_1fr_5rem_5rem_1fr_1fr_5.75rem] items-center bg-zinc-800/20 border-t border-zinc-700/50"
      >
        <div></div>
        <div></div>
        <div class="pl-6 pr-3 py-3">
          <input
              ref="firstEl"
              v-model="draft.name"
              placeholder="Row name"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </div>
        <div class="px-3 py-3">
          <input
              v-model="draft.startTime"
              placeholder="00:00"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </div>
        <div class="px-3 py-3">
          <input
              v-model="draft.duration"
              placeholder="00:00"
              class="w-full bg-zinc-900 text-zinc-100 text-sm px-3 py-2 rounded-md border border-zinc-700 focus:outline-none focus:border-zinc-400 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </div>
        <div class="px-3 py-3 col-span-3">
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
        </div>
      </div>
    </div>

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