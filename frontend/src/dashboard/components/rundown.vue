<script setup lang="ts">
import { ref, nextTick } from "vue"
import type { Rundown, Page, Row } from "../useRundown.ts"
import RundownHeader from "./rundown-header.vue"
import RundownPage   from "./rundown-page.vue"
import { useReorder, DRAG_TYPE_PAGE } from "../useReorder.ts"

const props = defineProps<{ rundown: Rundown; activePageId?: string | null }>()
const emit = defineEmits<{
  save:         []
  rename:       [name: string]
  addPage:      [page: Page]
  removePage:   [pageId: string]
  reorder:      [pages: Page[]]
  renamePage:   [pageId: string, name: string]
  addRow:       [pageId: string, row: Row]
  updateRow:    [pageId: string, row: Row]
  removeRow:    [pageId: string, rowId: string]
  reorderRows:  [pageId: string, rows: Row[]]
  selectRow:    [row: Row]
  takeRow:      [row: Row]
  captureRow:   [pageId: string, row: Row]
  playPage:     [pageId: string]
}>()

const openPages = ref<Record<string, boolean>>({})

const { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd, moveUp, moveDown } =
    useReorder(() => props.rundown.pages, (pages) => emit("reorder", pages), DRAG_TYPE_PAGE)

const addingPage     = ref(false)
const newPageName    = ref("")
const newPageInputEl = ref<HTMLInputElement | null>(null)
const committed      = ref(false)

async function startAddPage() {
  committed.value   = false
  addingPage.value  = true
  newPageName.value = ""
  await nextTick()
  newPageInputEl.value?.focus()
}

function commitAddPage() {
  if (committed.value) return
  committed.value  = true
  addingPage.value = false
  const name = newPageName.value.trim()
  if (!name) return
  const page: Page = { id: crypto.randomUUID(), name, rows: [], order: 0 }
  emit("addPage", page)
  openPages.value[page.id] = true
}

function cancelAddPage() {
  committed.value  = true
  addingPage.value = false
}
</script>

<template>
  <div class="flex flex-col h-full select-none">

    <RundownHeader
        :name="rundown.name"
        :page-count="rundown.pages.length"
        @save="emit('save')"
        @rename="(name: string) => emit('rename', name)"
    />

    <div class="flex-1 overflow-y-auto divide-y divide-zinc-800/60">

      <div v-if="rundown.pages.length === 0 && !addingPage"
           class="px-5 py-10 text-center text-xs text-zinc-600 font-mono">
        No pages yet
      </div>

      <RundownPage
          v-for="(page, index) in rundown.pages"
          :key="page.id"
          :page="page"
          :index="index"
          :is-last="index === rundown.pages.length - 1"
          :is-open="!!openPages[page.id]"
          :is-dragging="draggingId === page.id"
          :is-drag-over="dragOverId === page.id"
          :is-active-page="activePageId === page.id"
          @toggle="openPages[page.id] = !openPages[page.id]"
          @rename="emit('renamePage', page.id, $event)"
          @remove="emit('removePage', page.id)"
          @move-up="moveUp(index)"
          @move-down="moveDown(index)"
          @dragstart="onDragStart($event, page.id)"
          @dragover="onDragOver($event, page.id)"
          @drop="onDrop($event, page.id)"
          @dragend="onDragEnd"
          @add-row="emit('addRow', page.id, $event)"
          @update-row="emit('updateRow', page.id, $event)"
          @remove-row="emit('removeRow', page.id, $event)"
          @reorder-rows="emit('reorderRows', page.id, $event)"
          @select-row="emit('selectRow', $event)"
          @take-row="emit('takeRow', $event)"
          @capture-row="emit('captureRow', page.id, $event)"
          @play-page="emit('playPage', page.id)"
      />

      <div
          v-if="addingPage"
          class="flex items-center gap-2 px-4 py-2.5 bg-zinc-800/20 border-t border-zinc-700/40 font-mono text-sm"
      >
        <input
            ref="newPageInputEl"
            v-model="newPageName"
            placeholder="Page name…"
            class="flex-1 bg-transparent text-zinc-100 text-sm placeholder-zinc-600 focus:outline-none"
            @keydown.enter="commitAddPage"
            @keydown.esc="cancelAddPage"
            @blur="commitAddPage"
        />
        <span class="text-xs text-zinc-600">↵ confirm · esc cancel</span>
      </div>
    </div>

    <div class="shrink-0 px-3 py-2 border-t border-zinc-800 bg-zinc-900">
      <button
          class="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-zinc-200 transition-colors py-1 px-2 rounded hover:bg-zinc-800 font-mono"
          @click="startAddPage"
      >+ Add page</button>
    </div>
  </div>
</template>