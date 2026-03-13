<script setup lang="ts">
import { onBeforeUnmount, onMounted } from "vue"
import { createSocket } from "./socket.ts"
import { useSocket } from "./useSocket.ts"
import { groups, useScenes } from "./useScenes.ts"
import { type Page, type Row, useRundown } from "./useRundown.ts"
import SceneGroupComponent from "./components/scene-group.vue"
import RundownComponent from "./components/rundown.vue"

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
  deleteRundown,
  addPage,
  removePage,
  addRow,
} = useRundown(socket)
</script>

<template>
  <nav class="flex flex-wrap gap-6 px-4 py-3 border-b border-zinc-800 bg-zinc-900">
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

  <main class="flex h-full">
    <!-- Rundown sidebar -->
    <aside class="w-64 shrink-0 border-r border-zinc-800 bg-zinc-900 flex flex-col">
      <div class="flex items-center justify-between px-4 py-3 border-b border-zinc-800">
        <span class="text-sm font-medium text-zinc-300">Rundowns</span>
        <button
            class="text-xs text-zinc-400 hover:text-white transition-colors"
            @click="createRundown('New Rundown')"
        >
          + New
        </button>
      </div>

      <ul class="flex-1 overflow-y-auto py-1">
        <li
            v-for="meta in rundownState.index"
            :key="meta.id"
            class="flex items-center gap-2 px-4 py-2 cursor-pointer hover:bg-zinc-800 transition-colors"
            :class="{ 'bg-zinc-800': rundownState.current?.id === meta.id }"
            @click="loadRundown(meta.id)"
        >
          <span class="flex-1 truncate text-sm text-zinc-300">{{ meta.name }}</span>
          <button
              class="text-zinc-600 hover:text-red-400 transition-colors text-xs shrink-0"
              @click.stop="deleteRundown(meta.id)"
          >✕</button>
        </li>

        <li v-if="rundownState.index.length === 0" class="px-4 py-3 text-xs text-zinc-600">
          No rundowns yet
        </li>
      </ul>
    </aside>

    <!-- Rundown content -->
    <section class="flex-1 overflow-y-auto">
      <div v-if="rundownState.loading" class="flex items-center justify-center h-32 text-zinc-500 text-sm">
        Loading…
      </div>

      <div v-else-if="rundownState.error" class="px-6 py-4 text-red-400 text-sm">
        {{ rundownState.error }}
      </div>

      <RundownComponent
          v-else-if="rundownState.current"
          :rundown="rundownState.current"
          @save="saveRundown()"
          @rename="(name: string) => renameRundown(rundownState.current!.id, name)"
          @add-page="(page: Page) => { addPage(page); saveRundown() }"
          @remove-page="(pageId: string) => { removePage(pageId); saveRundown() }"
          @reorder="(pages: Page[]) => { rundownState.current!.pages = pages; saveRundown() }"
          @rename-page="(pageId: string, name: string) => {
              const page = rundownState.current?.pages.find(p => p.id === pageId)
              if (page) { page.name = name; saveRundown() }
          }"
          @add-row="(pageId: string, row: Row) => { addRow(pageId, row); saveRundown() }"
      />

      <div v-else class="flex items-center justify-center h-32 text-zinc-600 text-sm">
        Select a rundown to get started
      </div>
    </section>
  </main>
</template>