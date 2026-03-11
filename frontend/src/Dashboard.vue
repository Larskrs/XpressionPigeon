<!-- Dashboard.vue -->
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive } from "vue"
import { createSocket } from "./dashboard.socket.ts"
import { useDashboardSocket } from "./useDashboardSocket.ts"
import { store } from "./shared/store.ts"
import { groups } from "./groups.ts"
import SceneInput from "@/SceneInput.vue"
import SceneCard from "@/SceneCard.vue";

const socket = createSocket()
useDashboardSocket(socket)

onMounted(() => socket.connect())
onBeforeUnmount(() => socket.disconnect())

const sceneStates = reactive<Record<string, boolean>>({})

const scenes = computed(() => {
  const grouped: Record<string, Record<string, string>> = {}
  for (const [key, value] of Object.entries(store.values)) {
    const underscore = key.lastIndexOf("_")
    const sceneId = key.substring(0, underscore)
    const field = key.substring(underscore + 1)
    if (!grouped[sceneId]) grouped[sceneId] = {}
    grouped[sceneId][field] = value
  }
  return grouped
})

const ungroupedSceneIds = computed(() => {
  const allGrouped = new Set(groups.flatMap(g => g.sceneIds))
  return Object.keys(scenes.value).filter(id => !allGrouped.has(id))
})

const takeScene  = (id: string) => { sceneStates[id] = true;  socket.send({ type: "TakeScene", sceneId: id }) }
const outScene   = (id: string) => { sceneStates[id] = false; socket.send({ type: "OutScene",  sceneId: id }) }
const takeGroup  = (ids: string[]) => ids.forEach(takeScene)
const outGroup   = (ids: string[]) => ids.forEach(outScene)
const updateField = (sceneId: string, key: string, value: string) => {
  store.updateValue(`${sceneId}_${key}`, value)
  socket.send({ type: "UpdateContent", sceneId, key, value })
}
</script>

<template>
  <div class="min-h-screen bg-zinc-950 text-zinc-100 font-mono p-8">

    <!-- Header -->
    <div class="mb-10 border-b border-zinc-800 pb-6 flex items-end justify-between">
      <div>
        <p class="text-xs tracking-[0.3em] text-zinc-500 uppercase mb-1">XPression Pigeon</p>
        <h1 class="text-3xl font-bold tracking-tight text-white">Nyheter <span class="text-zinc-500">BA25</span></h1>
      </div>
      <div class="flex items-center gap-2 text-xs text-zinc-500">
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
        LIVE
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="Object.keys(scenes).length === 0"
         class="flex flex-col items-center justify-center h-64 text-zinc-600 gap-3">
      <svg class="w-8 h-8 animate-spin" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
      </svg>
      <span class="text-sm tracking-widest uppercase">Waiting for data...</span>
    </div>

    <div class="flex flex-col gap-10">

      <!-- Groups -->
      <div v-for="group in groups" :key="group.label">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <span class="w-1 h-5 rounded-sm bg-amber-400"></span>
            <h2 class="text-xs tracking-[0.25em] uppercase text-zinc-400 font-semibold">{{ group.label }}</h2>
          </div>
          <div class="flex rounded-lg overflow-hidden border border-zinc-700 text-xs font-semibold tracking-widest uppercase">
            <button @click="takeGroup(group.sceneIds)" class="px-5 py-2 bg-blue-600 hover:bg-blue-500 text-white transition-colors">▶ Take All</button>
            <div class="w-px bg-zinc-700"></div>
            <button @click="outGroup(group.sceneIds)"  class="px-5 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 transition-colors">◀ Out All</button>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <template v-for="sceneId in group.sceneIds.filter(id => scenes[id])" :key="sceneId">
            <SceneCard
                :scene-id="sceneId"
                :fields="scenes[sceneId]"
                :is-on="sceneStates[sceneId]"
                @take="takeScene"
                @out="outScene"
                @update="updateField"
            />
          </template>
        </div>
      </div>

      <!-- Ungrouped -->
      <div v-if="ungroupedSceneIds.length > 0">
        <div class="flex items-center gap-3 mb-4">
          <span class="w-1 h-5 rounded-sm bg-zinc-600"></span>
          <h2 class="text-xs tracking-[0.25em] uppercase text-zinc-500 font-semibold">Other</h2>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          <SceneCard
              v-for="sceneId in ungroupedSceneIds"
              :key="sceneId"
              :scene-id="sceneId"
              :fields="scenes[sceneId]"
              :is-on="sceneStates[sceneId]"
              @take="takeScene"
              @out="outScene"
              @update="updateField"
          />
        </div>
      </div>

    </div>
  </div>
</template>