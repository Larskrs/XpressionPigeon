<script setup lang="ts">
import { ref, nextTick, computed } from "vue"
import type { Row } from "../useRundown.ts"
import { Icon } from "@iconify/vue"

const props = defineProps<{
  row: Row
  index: number
  status?: "live" | "done" | "upcoming"
}>()

const emit = defineEmits<{
  update:    [row: Row]
  removeRow: [rowId: string]
  select:    [row: Row]
  take:      [row: Row]
  capture:   [row: Row]
  dragstart: [event: DragEvent, row: Row]
  dragend:   []
}>()

type EditField = "name" | "startTime" | "duration"

const editingField = ref<EditField | null>(null)
const editValue = ref("")
const inputEl = ref<HTMLInputElement | null>(null)

async function startEdit(field: EditField) {
  editingField.value = field
  editValue.value = field === "name"
      ? props.row.name
      : formatSeconds(props.row[field])
  await nextTick()
  inputEl.value?.focus()
  inputEl.value?.select()
}

function commitEdit() {
  const field = editingField.value
  if (!field) return
  editingField.value = null

  const updated: Row = {
    ...props.row,
    values: props.row.values.map(v => ({ ...v })),
  }

  if (field === "name") {
    const trimmed = editValue.value.trim()
    if (trimmed === updated.name) return
    updated.name = trimmed
  } else {
    const seconds = parseTimeInput(editValue.value)
    if (seconds === updated[field]) return
    updated[field] = seconds
  }

  emit("update", updated)
}

function cancelEdit() {
  editingField.value = null
}

function formatSeconds(total: number): string {
  if (!total && total !== 0) return "0:00"
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n: number) => String(n).padStart(2, "0")
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

function parseTimeInput(input: string): number {
  const t = input.trim()
  if (!t) return 0
  if (/^\d+$/.test(t)) return Math.max(0, parseInt(t, 10))
  const parts = t.split(":").map(p => parseInt(p, 10) || 0)
  if (!parts[0] || !parts[1] || !parts[2]) return 0
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2]
  if (parts.length === 2) return parts[0] * 60 + parts[1]
  return 0
}

function onDragStart(e: DragEvent) {
  e.dataTransfer?.setData("text/plain", props.row.id)
  e.dataTransfer!.effectAllowed = "move"
  emit("dragstart", e, props.row)
}

function onDragEnd() {
  emit("dragend")
}

const displayDuration = computed(() => formatSeconds(props.row.duration))
const displayStart    = computed(() => formatSeconds(props.row.startTime))
const isUntitled      = computed(() => !props.row.name?.trim())
</script>

<template>
  <!-- Drag handle -->
  <div
      class="flex cursor-grab items-center justify-center px-1 text-zinc-700 transition-colors hover:text-zinc-400 active:cursor-grabbing"
      draggable="true"
      @dragstart="onDragStart"
      @dragend="onDragEnd"
  >
    <Icon icon="material-symbols:drag-indicator" class="size-4" />
  </div>

  <!-- Select button (left side) -->
  <div class="flex items-center gap-2 pl-1 pr-2 opacity-0 transition-opacity group-hover/row:opacity-100 group-focus-within/row:opacity-100">
    <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md bg-zinc-300/10 px-2.5 py-1 text-xs font-medium text-zinc-400 transition-colors hover:bg-zinc-500/20 hover:text-zinc-300"
        title="Select row"
        @click.stop="emit('select', row)"
    >
      <Icon icon="tabler:arrow-right" class="size-3.5 shrink-0" />
      <span>Select</span>
    </button>

    <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md bg-blue-500/10 px-2.5 py-1 text-xs font-medium text-blue-400 transition-colors hover:bg-blue-500/20 hover:text-blue-300"
        title="Take values"
        @click.stop="emit('take', row)"
    >
      <Icon icon="tabler:play" class="size-3.5 shrink-0" />
      <span>Take</span>
    </button>
  </div>

  <!-- Name -->
  <div class="flex min-w-0 items-center px-2 py-1.5">
    <input
        v-if="editingField === 'name'"
        ref="inputEl"
        v-model="editValue"
        class="w-full rounded border border-blue-500/60 bg-zinc-900 px-2.5 py-1 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-blue-400"
        placeholder="Row name"
        @blur="commitEdit"
        @keydown.enter.stop="commitEdit"
        @keydown.esc.stop="cancelEdit"
        @click.stop
    />
    <button
        v-else
        type="button"
        class="group/name flex w-full min-w-0 items-center gap-1.5 rounded px-2 py-1 text-left transition-colors hover:bg-white/5"
        title="Click to rename"
        @click.stop="startEdit('name')"
    >
      <span
          class="flex-1 truncate text-sm leading-none"
          :class="isUntitled ? 'italic text-zinc-600' : 'text-zinc-100'"
      >
        {{ row.name || "Untitled row" }}
      </span>
      <Icon
          icon="lucide:pencil"
          class="size-3 shrink-0 text-zinc-600 opacity-0 transition-opacity group-hover/name:opacity-100"
      />
    </button>
  </div>

  <!-- Start time -->
  <div class="flex items-center justify-end px-2 py-1.5">
    <input
        v-if="editingField === 'startTime'"
        ref="inputEl"
        v-model="editValue"
        class="w-20 rounded border border-blue-500/60 bg-zinc-900 px-2 py-1 text-right font-mono text-xs tabular-nums text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-blue-400"
        placeholder="0:00"
        @blur="commitEdit"
        @keydown.enter.stop="commitEdit"
        @keydown.esc.stop="cancelEdit"
        @click.stop
    />
    <button
        v-else
        type="button"
        class="w-full rounded px-2 py-1 text-right font-mono text-xs tabular-nums transition-colors hover:bg-white/5"
        :class="row.startTime ? 'text-zinc-400' : 'text-zinc-700'"
        title="Edit start time"
        @click.stop="startEdit('startTime')"
    >
      {{ displayStart }}
    </button>
  </div>

  <!-- Duration -->
  <div class="flex items-center justify-end px-2 py-1.5">
    <input
        v-if="editingField === 'duration'"
        ref="inputEl"
        v-model="editValue"
        class="w-20 rounded border border-blue-500/60 bg-zinc-900 px-2 py-1 text-right font-mono text-xs tabular-nums text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-blue-400"
        placeholder="0:00"
        @blur="commitEdit"
        @keydown.enter.stop="commitEdit"
        @keydown.esc.stop="cancelEdit"
        @click.stop
    />
    <button
        v-else
        type="button"
        class="w-full rounded px-2 py-1 text-right font-mono text-xs tabular-nums transition-colors hover:bg-white/5"
        :class="row.duration ? 'text-zinc-400' : 'text-zinc-700'"
        title="Edit duration"
        @click.stop="startEdit('duration')"
    >
      {{ displayDuration }}
    </button>
  </div>

  <!-- Badges -->
  <div class="flex items-center gap-1.5 px-2 py-1.5">
    <span
        v-if="row.notes"
        class="inline-flex size-5 items-center justify-center rounded bg-amber-500/10 text-amber-500"
        title="Has notes"
    >
      <Icon icon="tabler:note" class="size-3" />
    </span>
    <span
        v-if="row.values.length"
        class="inline-flex min-w-[1.5rem] items-center justify-center rounded bg-zinc-800 px-1.5 py-0.5 font-mono text-[10px] tabular-nums text-zinc-400"
        :title="`${row.values.length} parameter(s)`"
    >
      {{ row.values.length }}p
    </span>
  </div>

  <!-- Capture button (right side) -->
  <div class="flex items-center justify-end pr-2 gap-2 opacity-0 transition-opacity group-hover/row:opacity-100 group-focus-within/row:opacity-100">
    <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-400 transition-colors hover:bg-emerald-500/20 hover:text-emerald-300"
        title="Capture row"
        @click.stop="emit('capture', row)"
    >
      <Icon icon="tabler:camera" class="size-3.5 shrink-0" />
      <span>Capture</span>
    </button>
    <button
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md bg-red-500/10 px-2.5 py-1 text-xs font-medium text-red-400 transition-colors hover:bg-red-500/20 hover:text-red-300"
        title="Capture row"
        @click.stop="emit('removeRow', row.id)"
    >
      <Icon icon="tabler:trash" class="size-3.5 shrink-0" />
    </button>
  </div>
</template>