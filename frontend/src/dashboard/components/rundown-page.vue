<script setup lang="ts">
import { ref, nextTick } from "vue"
import type { Page, Row } from "../useRundown.ts"
import RundownPageRows from "./rundown-page-rows.vue"
import { Icon } from "@iconify/vue";

const props = defineProps<{
  page:          Page
  index:         number
  isLast:        boolean
  isOpen:        boolean
  isDragging:    boolean
  isDragOver:    boolean
  isActivePage?: boolean
}>()

const emit = defineEmits<{
  toggle:      []
  rename:      [name: string]
  remove:      []
  addRow:      [row: Row]
  updateRow:   [row: Row]
  removeRow:   [rowId: string]
  reorderRows: [rows: Row[]]
  selectRow:   [row: Row]
  takeRow:     [row: Row]
  captureRow:  [row: Row]
  playPage:    []
  moveUp:      []
  moveDown:    []
  dragstart:   [e: DragEvent]
  dragover:    [e: DragEvent]
  drop:        [e: DragEvent]
  dragend:     []
}>()

// ── Inline rename ─────────────────────────────────────────────────────────────
const editing      = ref(false)
const nameInput    = ref("")
const nameInputEl  = ref<HTMLInputElement | null>(null)

async function startRename() {
  editing.value   = true
  nameInput.value = props.page.name
  await nextTick()
  nameInputEl.value?.focus()
  nameInputEl.value?.select()
}

function commitRename() {
  editing.value = false
  if (nameInput.value.trim() && nameInput.value !== props.page.name)
    emit("rename", nameInput.value.trim())
}

// ── Delete confirmation ───────────────────────────────────────────────────────
const confirming = ref(false)

// ── Page drag — only initiated from the handle ────────────────────────────────
function onHandleDragStart(e: DragEvent) {
  emit("dragstart", e)
}
</script>

<template>
  <div
      class="group transition-opacity"
      :class="{ 'opacity-30': isDragging }"
      @dragover="emit('dragover', $event)"
      @drop="emit('drop', $event)"
      @dragend="emit('dragend')"
  >
    <!-- Drop indicator -->
    <div
        class="h-0.5 bg-blue-500 mx-5 transition-opacity duration-100"
        :class="isDragOver ? 'opacity-100' : 'opacity-0'"
    />

    <!-- Page header -->
    <div
        class="flex items-center gap-1 px-2 py-0 hover:bg-zinc-800/40 transition-colors"
        :class="{ 'bg-emerald-950/40 border-l-2 border-emerald-500': isActivePage }"
    >

      <!-- Drag handle -->
      <span
          draggable="true"
          class="cursor-grab active:cursor-grabbing text-zinc-700 hover:text-zinc-400 px-1 py-1 transition-colors shrink-0"
          title="Drag to reorder"
          @dragstart.stop="onHandleDragStart"
      >
        <svg viewBox="0 0 10 16" width="10" height="16" fill="currentColor">
          <circle cx="3" cy="3"  r="1.2"/><circle cx="7" cy="3"  r="1.2"/>
          <circle cx="3" cy="8"  r="1.2"/><circle cx="7" cy="8"  r="1.2"/>
          <circle cx="3" cy="13" r="1.2"/><circle cx="7" cy="13" r="1.2"/>
        </svg>
      </span>

      <!-- Chevron + name -->
      <button class="flex items-center gap-2 flex-1 py-2 text-left min-w-0" @click="emit('toggle')">
        <svg
            class="w-3 h-3 shrink-0 text-zinc-500 transition-transform duration-150"
            :class="{ 'rotate-90': isOpen }"
            viewBox="0 0 12 12" fill="none"
        >
          <path d="M4 2l4 4-4 4" stroke="currentColor" stroke-width="1.5"
                stroke-linecap="round" stroke-linejoin="round"/>
        </svg>

        <template v-if="editing">
          <input
              ref="nameInputEl"
              v-model="nameInput"
              class="flex-1 bg-zinc-800 text-zinc-100 px-2 py-0.5 rounded border border-zinc-600 focus:outline-none focus:border-zinc-400 text-sm"
              @blur="commitRename"
              @keydown.enter.stop="commitRename"
              @keydown.esc.stop="editing = false"
              @click.stop
          />
        </template>
        <template v-else>
          <span class="flex-1 truncate text-zinc-200">{{ page.name }}</span>
        </template>
      </button>

      <!-- Row count -->
      <span class="text-xs text-zinc-600 tabular-nums shrink-0 px-1">
        {{ page.rows.length }}r
      </span>

      <!-- Hover actions -->
      <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
        <button
            class="p-1 rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-700 transition-colors disabled:opacity-20 disabled:pointer-events-none"
            :disabled="index === 0"
            title="Move up"
            @click.stop="emit('moveUp')"
        >
          <svg viewBox="0 0 12 12" width="12" height="12" fill="none">
            <path d="M2 8l4-4 4 4" stroke="currentColor" stroke-width="1.5"
                  stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <button
            class="p-1 rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-700 transition-colors disabled:opacity-20 disabled:pointer-events-none"
            :disabled="isLast"
            title="Move down"
            @click.stop="emit('moveDown')"
        >
          <svg viewBox="0 0 12 12" width="12" height="12" fill="none">
            <path d="M2 4l4 4 4-4" stroke="currentColor" stroke-width="1.5"
                  stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <button
            class="p-1 rounded text-zinc-600 hover:text-emerald-400 hover:bg-zinc-700 transition-colors"
            :class="{ 'text-emerald-500': isActivePage }"
            title="Play this page"
            @click.stop="emit('playPage')"
        ><Icon class="size-4" icon="tabler:player-play" />
        </button>

        <button
            class="p-1 rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-700 transition-colors"
            title="Rename page"
            @click.stop="startRename"
        ><Icon class="size-4" icon="tabler:edit" />
        </button>

        <button
            class="p-1 rounded text-zinc-600 hover:text-red-400 hover:bg-zinc-700 transition-colors"
            title="Delete page"
            @click.stop="confirming = true"
        ><Icon class="size-4" icon="tabler:trash" />
        </button>
      </div>
    </div>

    <!-- Delete confirmation strip -->
    <div
        v-if="confirming"
        class="flex items-center gap-3 pl-10 pr-4 py-2 bg-red-950/40 border-t border-red-900/30 text-xs font-mono"
    >
      <span class="flex-1 text-red-300">Delete "{{ page.name }}"?</span>
      <button
          class="px-3 py-1 rounded bg-red-700 hover:bg-red-600 text-white transition-colors"
          @click="emit('remove'); confirming = false"
      >Delete</button>
      <button
          class="px-3 py-1 rounded bg-zinc-700 hover:bg-zinc-600 text-zinc-300 transition-colors"
          @click="confirming = false"
      >Cancel</button>
    </div>

    <!-- Expanded rows -->
    <RundownPageRows
        v-if="isOpen"
        :page="page"
        @add-row="emit('addRow', $event)"
        @update-row="emit('updateRow', $event)"
        @remove-row="emit('removeRow', $event)"
        @reorder-rows="emit('reorderRows', $event)"
        @select-row="emit('selectRow', $event)"
        @take-row="emit('takeRow', $event)"
        @capture-row="emit('captureRow', $event)"
    />
  </div>
</template>