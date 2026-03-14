<script setup lang="ts">
import { onBeforeUnmount, onMounted } from "vue"
import { createSocket } from "./socket.ts"
import { useSocket } from "./useSocket.ts"
import { groups, useScenes } from "./useScenes.ts"
import { type Page, type Row, useRundown } from "./useRundown.ts"
import { usePagePlayer } from "./usePagePlayer.ts"
import SceneGroupComponent from "./components/scene-group.vue"
import RundownComponent from "./components/rundown.vue"
import PagePlayerBar from "./components/page-player-bar.vue"

const socket = createSocket()
useSocket(socket)

onMounted(() => {
  socket.connect()
  listRundowns()
})
onBeforeUnmount(() => socket.disconnect())

const { scenes, sceneStates, takeScene, outScene, updateField, handleFlip } = useScenes(socket)
const {
  state: rundownState,
  listRundowns,
  loadRundown,
  saveRundown,
  createRundown,
  renameRundown,
  addPage,
  removePage,
  addRow,
  updateRow,
} = useRundown(socket)

// ── Page player ───────────────────────────────────────────────────────────────

const player = usePagePlayer({ scenes, updateField, takeScene, outScene })

// ── Stable event handlers ─────────────────────────────────────────────────────

function onRename(name: string) {
  renameRundown(rundownState.current!.id, name)
}

function onAddPage(page: Page) {
  addPage(page)
  saveRundown()
}

function onRemovePage(pageId: string) {
  removePage(pageId)
  saveRundown()
}

function onReorder(pages: Page[]) {
  rundownState.current!.pages = pages
  saveRundown()
}

function onRenamePage(pageId: string, name: string) {
  const page = rundownState.current?.pages.find(p => p.id === pageId)
  if (page) { page.name = name; saveRundown() }
}

function onAddRow(pageId: string, row: Row) {
  addRow(pageId, row)
  saveRundown()
}

function onUpdateRow(pageId: string, row: Row) {
  const page   = rundownState.current?.pages.find(p => p.id === pageId)
  const target = page?.rows.find(r => r.id === row.id)
  if (target) {
    target.name      = row.name
    target.startTime = row.startTime
    target.duration  = row.duration
    target.notes     = row.notes
    // do NOT reassign target.values — leave the reactive array in place
  }
  saveRundown()
}

function onReorderRows(pageId: string, rows: Row[]) {
  const page = rundownState.current?.pages.find(p => p.id === pageId)
  if (!page) return
  page.rows = rows
  saveRundown()
}

function onSelectRow(row: Row) {
  const rowValues = new Map(row.values.map(p => [p.name, p.value]))
  for (const [sceneId, fields] of Object.entries(scenes.value)) {
    for (const [field, currentValue] of Object.entries(fields)) {
      const key = `${sceneId}.${field}`
      const newValue = rowValues.get(key) ?? ""
      if (newValue === currentValue) continue
      updateField(sceneId, field, newValue)
    }
  }
}

function onCaptureRow(pageId: string, row: Row) {
  console.log("Captured")
  const captured = Object.entries(scenes.value).flatMap(([sceneId, fields]) =>
      Object.entries(fields)
          .filter(([, value]) => value !== "")
          .map(([field, value]) => ({ name: `${sceneId}.${field}`, value }))
  )
  console.log(captured)
  const page   = rundownState.current?.pages.find(p => p.id === pageId)
  const target = page?.rows.find(r => r.id === row.id)
  if (target) {
    target.values = captured
    saveRundown()
  }
}

function onPlayPage(pageId: string) {
  const page = rundownState.current?.pages.find(p => p.id === pageId)
  if (!page) return
  player.loadPage(page)
  player.play()
}
</script>

<template>
  <nav class="flex flex-wrap gap-6 px-2 py-3">
    <SceneGroupComponent
        v-for="group in groups"
        :key="group.label"
        :group="group"
        :scenes="scenes"
        :scene-states="sceneStates"
        @take="takeScene"
        @out="outScene"
        @update="updateField"
        @flip="handleFlip"
    />
  </nav>

  <main class="flex gap-2 h-full p-2">
    <!-- Rundown sidebar -->
    <aside class="rounded-lg border border-border w-64 shrink-0 bg-surface flex flex-col">
      <div class="flex items-center justify-between px-4 py-3 border-b border-zinc-800">
        <span class="text-sm font-medium text-zinc-300">Rundowns</span>
        <button
            class="text-xs text-zinc-400 hover:text-white transition-colors"
            @click="createRundown('New Rundown')"
        >
          + New
        </button>
      </div>

      <ul class="flex-1 overflow-y-auto p-1">
        <li
            v-for="meta in rundownState.index"
            :key="meta.id"
            class="flex items-center rounded gap-2 px-4 py-2 cursor-pointer transition-colors"
            :class="{ 'bg-surface-elevated': rundownState.current?.id === meta.id }"
            @click="loadRundown(meta.id)"
        >
          <span class="flex-1 truncate text-sm text-zinc-300">{{ meta.name }}</span>
          <span class="flex-1 truncate text-sm text-zinc-300">{{ meta.id }}</span>
        </li>

        <li v-if="rundownState.index.length === 0" class="px-4 py-3 text-xs text-zinc-600">
          No rundowns yet
        </li>
      </ul>
    </aside>

    <!-- Rundown content -->
    <section class="rounded-lg border border-border flex-1 overflow-y-auto">
      <div v-if="rundownState.loading && rundownState.current === null" class="flex items-center justify-center h-32 text-sm">
        Loading…
      </div>

      <div v-else-if="rundownState.error" class="px-6 py-4 text-red-400 text-sm">
        {{ rundownState.error }}
      </div>

      <RundownComponent
          v-else-if="rundownState.current"
          :rundown="rundownState.current"
          :active-page-id="player.page.value?.id ?? null"
          @save="saveRundown"
          @rename="onRename"
          @add-page="onAddPage"
          @remove-page="onRemovePage"
          @reorder="onReorder"
          @rename-page="onRenamePage"
          @add-row="onAddRow"
          @update-row="onUpdateRow"
          @reorder-rows="onReorderRows"
          @select-row="onSelectRow"
          @capture-row="onCaptureRow"
          @play-page="onPlayPage"
      />

      <div v-else class="flex items-center justify-center h-32 text-zinc-600 text-sm">
        Select a rundown to get started
      </div>
    </section>
  </main>

  <!-- Player transport bar — rendered via Teleport to body, shown when a page is loaded -->
  <PagePlayerBar v-if="player.page.value" :player="player" />
</template>