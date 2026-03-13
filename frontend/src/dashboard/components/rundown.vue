<script setup lang="ts">
import { ref, nextTick } from "vue"
import type { Rundown, Page } from "../useRundown.ts"
import RundownHeader   from "./rundown-header.vue"
import RundownPage     from "./rundown-page.vue"
import { usePageReorder } from "../usePageReorder.ts"

const props = defineProps<{ rundown: Rundown }>()
const emit  = defineEmits<{
  save:       []
  rename:     [name: string]
  addPage:    [page: Page]
  removePage: [pageId: string]
  reorder:    [pages: Page[]]
  renamePage: [pageId: string, name: string]
}>()

// ── Accordion ─────────────────────────────────────────────────────────────────
const openPages = ref<Set<string>>(new Set())

function togglePage(id: string) {
  openPages.value.has(id) ? openPages.value.delete(id) : openPages.value.add(id)
}

// ── Reorder ───────────────────────────────────────────────────────────────────
const { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd, moveUp, moveDown } =
    usePageReorder(() => props.rundown.pages, (pages) => emit("reorder", pages))

// ── Add page ──────────────────────────────────────────────────────────────────
const addingPage     = ref(false)
const newPageName    = ref("")
const newPageInputEl = ref<HTMLInputElement | null>(null)
const committed      = ref(false)   // guard against blur firing after Enter

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
  openPages.value.add(page.id)
}

function cancelAddPage() {
  committed.value  = true   // prevent blur from committing after Esc
  addingPage.value = false
}
</script>

<template>
  <div class="flex flex-col h-full select-none">

    <RundownHeader
        :name="rundown.name"
        :page-count="rundown.pages.length"
        @save="emit('save')"
        @rename="(name) => emit('rename', name)"
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
          :is-open="openPages.has(page.id)"
          :is-dragging="draggingId === page.id"
          :is-drag-over="dragOverId === page.id"
          @toggle="togglePage(page.id)"
          @rename="(name) => emit('renamePage', page.id, name)"
          @remove="emit('removePage', page.id)"
          @move-up="moveUp(index)"
          @move-down="moveDown(index)"
          @dragstart="onDragStart($event, page.id)"
          @dragover="onDragOver($event, page.id)"
          @drop="onDrop($event, page.id)"
          @dragend="onDragEnd"
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