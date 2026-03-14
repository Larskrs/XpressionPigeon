<script setup lang="ts">
import { ref, nextTick, computed, watchEffect } from "vue"
import type { Rundown, Page, Row } from "../useRundown.ts"
import RundownHeader from "./rundown-header.vue"
import RundownPage   from "./rundown-page.vue"
import { useReorder } from "../useReorder.ts"

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
  reorderRows:  [pageId: string, rows: Row[]]
  selectRow:    [row: Row]
  captureRow:   [pageId: string, row: Row]
  playPage:     [pageId: string]
}>()

// ── Accordion ─────────────────────────────────────────────────────────────────
const openPages = ref<Record<string, boolean>>({})

function togglePage(id: string) {
  openPages.value[id] = !openPages.value[id]
}

function isOpen(id: string) {
  return !!openPages.value[id]
}

// ── Reorder ───────────────────────────────────────────────────────────────────
const { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd, moveUp, moveDown } =
    useReorder(() => props.rundown.pages, (pages) => emit("reorder", pages))

// ── Per-page cached handlers ──────────────────────────────────────────────────
interface PageHandlers {
  toggle:      () => void
  rename:      (name: string) => void
  remove:      () => void
  addRow:      (row: Row) => void
  updateRow:   (row: Row) => void
  reorderRows: (rows: Row[]) => void
  selectRow:   (row: Row) => void
  captureRow:  (row: Row) => void
  playPage:    () => void
  moveUp:      (index: number) => void
  moveDown:    (index: number) => void
  dragstart:   (e: DragEvent) => void
  dragover:    (e: DragEvent) => void
  drop:        (e: DragEvent) => void
}

const handlerCache = new Map<string, PageHandlers>()

function handlersFor(pageId: string): PageHandlers {
  if (!handlerCache.has(pageId)) {
    handlerCache.set(pageId, {
      toggle:      ()     => togglePage(pageId),
      rename:      (name) => emit("renamePage", pageId, name),
      remove:      ()     => emit("removePage", pageId),
      addRow:      (row)  => emit("addRow", pageId, row),
      updateRow:   (row)  => emit("updateRow", pageId, row),
      reorderRows: (rows) => emit("reorderRows", pageId, rows),
      selectRow:   (row)  => emit("selectRow", row),
      captureRow:  (row)  => emit("captureRow", pageId, row),
      playPage:    ()     => emit("playPage", pageId),
      moveUp:      (i)    => moveUp(i),
      moveDown:    (i)    => moveDown(i),
      dragstart:   (e)    => onDragStart(e, pageId),
      dragover:    (e)    => onDragOver(e, pageId),
      drop:        (e)    => onDrop(e, pageId),
    })
  }
  return handlerCache.get(pageId)!
}

// Clean up handlers for pages that no longer exist
const pageIds = computed(() => new Set(props.rundown.pages.map(p => p.id)))
watchEffect(() => {
  for (const id of handlerCache.keys()) {
    if (!pageIds.value.has(id)) handlerCache.delete(id)
  }
})

// ── Add page ──────────────────────────────────────────────────────────────────
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
  const page: Page = { id: crypto.randomUUID(), name, rows: [] }
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

    <!-- Page list -->
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
          :is-open="isOpen(page.id)"
          :is-dragging="draggingId === page.id"
          :is-drag-over="dragOverId === page.id"
          :is-active-page="activePageId === page.id"
          @toggle="handlersFor(page.id).toggle()"
          @rename="handlersFor(page.id).rename($event)"
          @remove="handlersFor(page.id).remove()"
          @move-up="moveUp(index)"
          @move-down="moveDown(index)"
          @dragstart="handlersFor(page.id).dragstart($event)"
          @dragover="handlersFor(page.id).dragover($event)"
          @drop="handlersFor(page.id).drop($event)"
          @dragend="onDragEnd"
          @add-row="handlersFor(page.id).addRow($event)"
          @update-row="handlersFor(page.id).updateRow($event)"
          @reorder-rows="handlersFor(page.id).reorderRows($event)"
          @select-row="handlersFor(page.id).selectRow($event)"
          @capture-row="handlersFor(page.id).captureRow($event)"
          @play-page="handlersFor(page.id).playPage()"
      />

      <!-- Inline new page input -->
      <div
          v-if="addingPage"
          class="flex items-center gap-2 px-4 py-2.5 bg-zinc-800/20 border-t border-zinc-700/40 font-mono text-sm"
      >
        <svg viewBox="0 0 12 12" width="12" height="12" fill="none" class="text-zinc-500 shrink-0">
          <path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
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

    <!-- Footer -->
    <div class="shrink-0 px-3 py-2 border-t border-zinc-800 bg-zinc-900">
      <button
          class="flex items-center gap-1.5 text-xs text-zinc-500 hover:text-zinc-200 transition-colors py-1 px-2 rounded hover:bg-zinc-800 font-mono"
          @click="startAddPage"
      >
        <svg viewBox="0 0 12 12" width="12" height="12" fill="none">
          <path d="M6 2v8M2 6h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        Add page
      </button>
    </div>
  </div>
</template>