<script setup lang="ts">
import { computed, ref } from "vue"
import type { Row, Page } from "../useRundown.ts"
import RundownRow from "./rundown-row.vue"

const props = defineProps<{
  page: Page
}>()

const emit = defineEmits<{
  addRow:      [row: Row]
  updateRow:   [row: Row]
  removeRow: [rowId: string]
  reorderRows: [rows: Row[]]
  selectRow:   [row: Row]
  takeRow:     [row: Row]
  captureRow:  [row: Row]
}>()

const dragIndex = ref<number | null>(null)
const dropIndex = ref<number | null>(null)

const rowsSorted = computed(() =>
    [...props.page.rows].sort((a, b) => a.order - b.order)
)

function onDragStart(e: DragEvent, index: number) {
  dragIndex.value = index
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = "move"
    e.dataTransfer.setData("text/plain", String(index))
  }
}

function onDragOver(e: DragEvent, index: number) {
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = "move"
  dropIndex.value = index
}

function onDragLeave(e: DragEvent) {
  // Only clear if leaving the row entirely (not crossing child elements)
  if (!(e.currentTarget as HTMLElement).contains(e.relatedTarget as Node)) {
    dropIndex.value = null
  }
}

function onDrop(e: DragEvent, index: number) {
  e.preventDefault()

  if (dragIndex.value === null || dragIndex.value === index) {
    resetDrag()
    return
  }

  const reordered = [...rowsSorted.value]
  const [moved] = reordered.splice(dragIndex.value, 1)
  if (!moved) return
  reordered.splice(index, 0, moved)

  emit("reorderRows", reordered.map((row, i) => ({ ...row, order: i })))
  resetDrag()
}

function onDragEnd() {
  resetDrag()
}

function resetDrag() {
  dragIndex.value = null
  dropIndex.value = null
}

function addNewRow() {
  const maxOrder = props.page.rows.reduce((max, r) => {
    const o = Number(r.order)
    return isFinite(o) ? Math.max(max, o) : max
  }, -1)
  const row = {
    id: crypto.randomUUID(),
    order: maxOrder,
    name: "",
    startTime: 0,
    duration: 0,
    notes: "",
    values: [],
  }
  console.log(row.order)
  emit("addRow", row)
}
</script>

<template>
  <div class="w-full overflow-hidden">

    <div
        v-for="(row, i) in rowsSorted"
        :key="row.id"
        class="group/row relative grid h-10 grid-cols-[32px_160px_240px_80px_64px_72px_1fr] border-b border-zinc-800/60 last:border-b-0 transition-colors"
        :class="{
        'bg-zinc-800/40':           dropIndex === i && dragIndex !== i,
        'opacity-40':               dragIndex === i,
        'hover:bg-zinc-800/40':     dragIndex === null,
      }"
        @dragover="onDragOver($event, i)"
        @dragleave="onDragLeave($event)"
        @drop="onDrop($event, i)"
    >
      <!-- Drop indicator line -->
      <div
          v-if="dropIndex === i && dragIndex !== i"
          class="pointer-events-none absolute inset-x-0 top-0 h-0.5 bg-blue-500"
      />

      <RundownRow
          :row="row"
          :index="i"
          @update="emit('updateRow', $event)"
          @remove-row="emit('removeRow', $event)"
          @select="emit('selectRow', $event)"
          @capture="emit('captureRow', $event)"
          @dragstart="onDragStart($event, i)"
          @dragend="onDragEnd"
          @take="emit('takeRow', $event)"
      />
    </div>

    <div class="p-2">
      <button
          class="rounded-lg border border-zinc-800/60 bg-zinc-900 px-2 py-1 text-sm"
          @click.stop="addNewRow"
      >
        Create row
      </button>
    </div>

  </div>
</template>