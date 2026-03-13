<script setup lang="ts">
import { ref, nextTick } from "vue"
import type { Row } from "../useRundown.ts"
import { formatTime, duration } from "../useTimeFormat.ts"

defineProps<{ rows: Row[] }>()
const emit = defineEmits<{ addRow: [row: Row] }>()

// ── Add row ───────────────────────────────────────────────────────────────────
const adding   = ref(false)
const firstEl  = ref<HTMLInputElement | null>(null)

const draft = ref({
  name:      "",
  startTime: "",
  endTime:   "",
  notes:     "",
})

async function startAdd() {
  adding.value       = true
  draft.value        = { name: "", startTime: "", endTime: "", notes: "" }
  await nextTick()
  firstEl.value?.focus()
}

function parseTime(raw: string): number {
  const parts = raw.trim().split(":").map(Number)
  if (parts.some(isNaN)) return 0
  if (parts.length === 3) {
    return (((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 60 + (parts[2] ?? 0)) * 1000
  }
  if (parts.length === 2) {
    return ((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 1000
  }
  return (parts[0] ?? 0) * 1000
}

function commitAdd() {
  const name = draft.value.name.trim()
  adding.value = false
  if (!name) return
  emit("addRow", {
    name,
    startTime: parseTime(draft.value.startTime),
    endTime:   parseTime(draft.value.endTime),
    notes:     draft.value.notes.trim(),
    values:    [],
  })
}
</script>

<template>
  <div class="bg-zinc-950/30">
    <!-- Empty state (only when not adding) -->
    <div v-if="rows.length === 0 && !adding" class="pl-14 pr-5 py-3 text-xs text-zinc-600 italic">
      No rows on this page
    </div>

    <table class="w-full text-xs border-collapse">
      <thead v-if="rows.length > 0">
      <tr class="border-b border-zinc-800">
        <th class="pl-14 pr-3 py-1.5 text-left text-zinc-600 font-normal w-2/5">Name</th>
        <th class="px-3 py-1.5 text-left text-zinc-600 font-normal tabular-nums">Start</th>
        <th class="px-3 py-1.5 text-left text-zinc-600 font-normal tabular-nums">Dur</th>
        <th class="px-3 py-1.5 text-left text-zinc-600 font-normal">Notes</th>
        <th class="w-8"/>
      </tr>
      </thead>
      <tbody class="divide-y divide-zinc-800/40">
      <template v-for="row in rows" :key="row.name">
        <tr class="hover:bg-zinc-800/30 transition-colors group">
          <td class="pl-14 pr-3 py-1.5 text-zinc-300 truncate max-w-0 w-2/5">{{ row.name }}</td>
          <td class="px-3 py-1.5 text-zinc-500 tabular-nums whitespace-nowrap">{{ formatTime(row.startTime) }}</td>
          <td class="px-3 py-1.5 text-zinc-500 tabular-nums whitespace-nowrap">{{ duration(row.startTime, row.endTime) }}</td>
          <td class="px-3 py-1.5 text-zinc-500 truncate max-w-xs">{{ row.notes || "—" }}</td>
          <td class="pr-3 py-1.5 w-8"/>
        </tr>
        <!-- Parameter strip -->
        <tr v-if="row.values.length > 0" class="bg-zinc-900/40">
          <td colspan="5" class="pl-14 pr-5 py-2">
            <div class="flex flex-wrap gap-x-4 gap-y-1">
              <span class="w-full text-xs text-zinc-600 mb-0.5">{{ row.name }}</span>
              <span v-for="param in row.values" :key="param.name" class="text-xs">
                  <span class="text-zinc-600">{{ param.name }}:</span>
                  <span class="text-zinc-400 ml-1">{{ param.value || "—" }}</span>
                </span>
            </div>
          </td>
        </tr>
      </template>

      <!-- Add row form -->
      <tr v-if="adding" class="bg-zinc-800/20 border-t border-zinc-700/40">
        <td class="pl-14 pr-2 py-1.5">
          <input
              ref="firstEl"
              v-model="draft.name"
              placeholder="Row name"
              class="w-full bg-zinc-800 text-zinc-100 text-xs px-2 py-1 rounded border border-zinc-700 focus:outline-none focus:border-zinc-500 placeholder-zinc-600"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-2 py-1.5">
          <input
              v-model="draft.startTime"
              placeholder="00:00"
              class="w-20 bg-zinc-800 text-zinc-100 text-xs px-2 py-1 rounded border border-zinc-700 focus:outline-none focus:border-zinc-500 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-2 py-1.5">
          <input
              v-model="draft.endTime"
              placeholder="00:00"
              class="w-20 bg-zinc-800 text-zinc-100 text-xs px-2 py-1 rounded border border-zinc-700 focus:outline-none focus:border-zinc-500 placeholder-zinc-600 tabular-nums"
              @keydown.enter="commitAdd"
              @keydown.esc="adding = false"
          />
        </td>
        <td class="px-2 py-1.5" colspan="2">
          <div class="flex items-center gap-2">
            <input
                v-model="draft.notes"
                placeholder="Notes"
                class="flex-1 bg-zinc-800 text-zinc-100 text-xs px-2 py-1 rounded border border-zinc-700 focus:outline-none focus:border-zinc-500 placeholder-zinc-600"
                @keydown.enter="commitAdd"
                @keydown.esc="adding = false"
            />
            <button
                class="text-xs px-2 py-1 rounded bg-zinc-700 hover:bg-zinc-600 text-zinc-300 hover:text-white transition-colors shrink-0"
                @click="commitAdd"
            >Add</button>
            <button
                class="text-xs text-zinc-600 hover:text-zinc-400 transition-colors shrink-0"
                @click="adding = false"
            >✕</button>
          </div>
        </td>
      </tr>
      </tbody>
    </table>

    <!-- Add row button -->
    <div class="px-14 py-1.5 border-t border-zinc-800/40">
      <button
          class="flex items-center gap-1 text-xs text-zinc-600 hover:text-zinc-300 transition-colors py-1"
          @click="startAdd"
      >
        <svg viewBox="0 0 12 12" width="10" height="10" fill="none">
          <path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        Add row
      </button>
    </div>
  </div>
</template>