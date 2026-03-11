<!-- RundownTable.vue -->
<script setup lang="ts">
import { ref, computed } from "vue"
import type { RundownRow, Mode } from "./useRundown.ts"

const props = defineProps<{
  rows: RundownRow[]
  dataHeaders: string[]
  labelHeader: string | null
  selectedIdx: number | null
  selectedSet: Set<number>
  mode: Mode
}>()

const emit = defineEmits<{
  selectRow:        [idx: number]
  toggleBulkSelect: [idx: number]
  moveRow:          [from: number, to: number]
  insertAbove:      [idx: number]
  insertBelow:      [idx: number]
  deleteRow:        [idx: number]
  bulkDelete:       []
}>()

// ─── Drag to reorder ──────────────────────────────────────────────────────────
const dragFromIdx = ref<number | null>(null)
const dragOverIdx = ref<number | null>(null)

function onDragStart(e: DragEvent, idx: number) {
  dragFromIdx.value = idx
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = "move"
    e.dataTransfer.setData("text/plain", String(idx))
  }
}
function onDragOver(e: DragEvent, idx: number) {
  e.preventDefault()
  dragOverIdx.value = idx
}
function onDrop(e: DragEvent, toIdx: number) {
  e.preventDefault()
  if (dragFromIdx.value !== null && dragFromIdx.value !== toIdx)
    emit("moveRow", dragFromIdx.value, toIdx)
  dragFromIdx.value = null
  dragOverIdx.value = null
}
function onDragEnd() {
  dragFromIdx.value = null
  dragOverIdx.value = null
}

function handleRowClick(e: MouseEvent, idx: number) {
  if (props.mode === "editor" && e.shiftKey) emit("toggleBulkSelect", idx)
  else emit("selectRow", idx)
}

// ─── Scene groups ─────────────────────────────────────────────────────────────
const sceneGroups = computed(() => {
  const map = new Map<string, string[]>()
  for (const h of props.dataHeaders) {
    const u = h.lastIndexOf("_")
    const sid = h.substring(0, u)
    if (!map.has(sid)) map.set(sid, [])
    map.get(sid)!.push(h)
  }
  return [...map.entries()]
})

// Only scenes that have at least one value in this row
function filledScenes(row: RundownRow) {
  return sceneGroups.value
      .map(([sceneId, keys]) => ({
        sceneId,
        values: keys
            .map(k => row[k]?.trim())
            .filter(Boolean) as string[],
      }))
      .filter(s => s.values.length > 0)
}

function sceneLabel(sceneId: string) {
  // e.g. LeftPersonSuper → L.Person, ThemeSuper → Theme, Location → Location
  return sceneId.replace("Super", "").replace(/([A-Z])/g, " $1").trim()
}
</script>

<template>
  <div class="flex flex-col gap-2 font-mono text-xs">

    <!-- Bulk action bar -->
    <Transition
        enter-active-class="transition-all duration-150"
        leave-active-class="transition-all duration-100"
        enter-from-class="opacity-0 -translate-y-1"
        leave-to-class="opacity-0"
    >
      <div v-if="selectedSet.size > 0 && mode === 'editor'"
           class="flex items-center gap-3 px-3 py-2 mb-2 bg-red-950/40 border border-red-900/40 rounded-lg">
        <span class="text-red-400">{{ selectedSet.size }} row{{ selectedSet.size !== 1 ? "s" : "" }} selected</span>
        <span class="text-zinc-700">⇧ + click to multi-select</span>
        <button @click.stop="emit('bulkDelete')"
                class="ml-auto px-3 py-1 rounded border border-red-700/50 bg-red-900/30 hover:bg-red-900/50 text-red-400 tracking-widest uppercase transition-colors">
          ✕ Delete selected
        </button>
      </div>
    </Transition>

    <!-- Empty state -->
    <div v-if="!rows.length"
         class="flex items-center justify-center py-10 text-zinc-700 tracking-widest uppercase">
      No rows — switch to editor mode to add rows
    </div>

    <!-- Rows -->
    <div
        v-for="(row, i) in rows" :key="i"
        :draggable="mode === 'editor'"
        @dragstart="onDragStart($event, i)"
        @dragover.prevent="onDragOver($event, i)"
        @drop="onDrop($event, i)"
        @dragend="onDragEnd"
        @click="handleRowClick($event, i)"
        :class="[
        'gap-3 px-3 py-3 ',
        'group relative flex items-start rounded-md border transition-colors',
        mode === 'production' ? 'cursor-pointer' : 'cursor-default',
        selectedSet.has(i)   ? 'bg-red-950/30 border-red-800/40'
        : mode === 'production' && selectedIdx === i ? 'bg-blue-500/15 border-blue-500/75'
        : selectedIdx === i  ? 'bg-amber-500/10 border-amber-500/40'
        : dragOverIdx === i  ? 'bg-blue-950/40 border-blue-700/40'
        : 'border-zinc-800/50 bg-zinc-800/25 hover:bg-zinc-800/40 hover:border-zinc-700/20',
      ]"
    >
      <!-- Drag handle -->
      <span v-if="mode === 'editor'"
            class="mt-0.5 text-zinc-700 hover:text-zinc-400 cursor-grab active:cursor-grabbing select-none">
        ⠿
      </span>

      <!-- Row number -->
      <span class="mt-0.5 w-5 text-zinc-700 tabular-nums select-none text-right">{{ i + 1 }}</span>

      <!-- Label -->
      <span v-if="labelHeader"
            class="mt-0.5 w-28 text-zinc-300 font-semibold truncate"
            :title="row[labelHeader]">
        {{ row[labelHeader] || "—" }}
      </span>

      <!-- Filled scenes -->
      <div class="flex-1 flex flex-col justify-center gap-x-4 gap-y-1 min-w-0">
        <div v-for="scene in filledScenes(row)" :key="scene.sceneId"
             class="flex items-baseline gap-1.5 min-w-0">
          <!-- Scene label -->
          <span class="text-zinc-600 whitespace-nowrap flex-shrink-0">{{ sceneLabel(scene.sceneId) }}</span>
          <!-- Values joined -->
          <span class="text-zinc-300 truncate" :title="scene.values.join(' · ')">
            {{ scene.values.join(" · ") }}
          </span>
        </div>

        <!-- Totally empty row -->
        <span v-if="filledScenes(row).length === 0" class="text-zinc-700 italic">empty</span>
      </div>

      <!-- Row actions -->
      <div v-if="mode === 'editor'"
           class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity flex-shrink-0">
        <button @click.stop="emit('insertAbove', i)"
                class="w-5 h-5 flex items-center justify-center rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-700 transition-colors"
                title="Insert above">↑</button>
        <button @click.stop="emit('insertBelow', i)"
                class="w-5 h-5 flex items-center justify-center rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-700 transition-colors"
                title="Insert below">↓</button>
        <button @click.stop="emit('deleteRow', i)"
                class="w-5 h-5 flex items-center justify-center rounded text-zinc-600 hover:text-red-400 hover:bg-zinc-700 transition-colors"
                title="Delete">✕</button>
      </div>

      <!-- Active dot -->
      <span v-if="selectedIdx === i && !selectedSet.has(i)"
            class="mt-1 w-1.5 h-1.5 rounded-full bg-amber-400 flex-shrink-0" />
      <span v-else class="w-1.5 flex-shrink-0" />
    </div>

  </div>
</template>