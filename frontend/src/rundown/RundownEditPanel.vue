<!-- RundownEditPanel.vue -->
<script setup lang="ts">
import { computed, watch, ref } from "vue"
import type { RundownRow } from "./useRundown.ts"

const props = defineProps<{
  row: RundownRow | null
  rowIndex: number | null
  dataHeaders: string[]
  labelHeader: string | null
}>()

const emit = defineEmits<{
  update:           [flatKey: string, value: string]
  insertAbove:      []
  insertBelow:      []
  delete:           []
  replaceFromStore: []
}>()

// Local copy — buffered until blur or Enter
const localValues = ref<Record<string, string>>({})

watch(() => props.row, (row) => {
  localValues.value = row ? { ...row } : {}
}, { immediate: true, deep: true })

function commit(flatKey: string) {
  const val = localValues.value[flatKey] ?? ""
  if (val !== props.row?.[flatKey]) {
    emit("update", flatKey, val)
  }
}

// Group flat keys by sceneId
const groups = computed(() => {
  const map = new Map<string, string[]>()
  for (const flatKey of props.dataHeaders) {
    const u = flatKey.lastIndexOf("_")
    const sceneId = flatKey.substring(0, u)
    if (!map.has(sceneId)) map.set(sceneId, [])
    map.get(sceneId)!.push(flatKey)
  }
  return [...map.entries()].map(([sceneId, keys]) => ({ sceneId, keys }))
})

function fieldName(flatKey: string) {
  return flatKey.substring(flatKey.lastIndexOf("_") + 1)
}

function fromCamel(str: string) {
  return str.replace(/([A-Z])/g, " $1").trim()
}
</script>

<template>
  <div class="flex flex-col h-full font-mono text-sm overflow-hidden">

    <!-- Empty state -->
    <div v-if="row === null"
         class="flex flex-col items-center justify-center flex-1 gap-2 text-zinc-700 px-4 text-center">
      <svg class="w-7 h-7" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6M3 12a9 9 0 1018 0A9 9 0 003 12z"/>
      </svg>
      <span class="text-xs tracking-widest uppercase">Select a row to edit</span>
    </div>

    <template v-else>

      <!-- Panel header bar -->
      <div class="flex items-center justify-between gap-2 px-3 py-2.5 border-b border-zinc-800 bg-zinc-900/60 flex-shrink-0">
        <span class="text-xs text-zinc-500 tracking-widest uppercase">
          Row {{ rowIndex !== null ? rowIndex + 1 : "—" }}
        </span>
        <div class="flex items-center gap-1">
          <button @click="emit('insertAbove')"
                  class="h-6 px-2 rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors text-xs tracking-wide"
                  title="Insert above">↑</button>
          <button @click="emit('insertBelow')"
                  class="h-6 px-2 rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors text-xs tracking-wide"
                  title="Insert below">↓</button>
          <div class="w-px h-3.5 bg-zinc-700 mx-0.5"></div>
          <button @click="emit('replaceFromStore')"
                  class="h-6 px-2 rounded text-amber-600/70 hover:text-amber-400 hover:bg-zinc-800 transition-colors text-xs tracking-wide"
                  title="Fill all fields from current live values">↩ Live</button>
          <div class="w-px h-3.5 bg-zinc-700 mx-0.5"></div>
          <button @click="emit('delete')"
                  class="h-6 px-2 rounded text-zinc-700 hover:text-red-400 hover:bg-zinc-800 transition-colors text-xs tracking-wide"
                  title="Delete row">✕</button>
        </div>
      </div>

      <!-- Scrollable fields -->
      <div class="flex-1 overflow-y-auto">

        <!-- Label field (e.g. "cue") -->
        <div v-if="labelHeader" class="px-3 pt-4 pb-3 border-b border-zinc-800/50">
          <div class="text-xs font-semibold text-zinc-400 tracking-widest uppercase mb-2">
            {{ labelHeader }}
          </div>
          <input
              v-model="localValues[labelHeader]"
              @blur="commit(labelHeader)"
              @keydown.enter="($event.target as HTMLInputElement).blur()"
              :placeholder="labelHeader"
              class="w-full bg-zinc-900 border border-zinc-700 rounded-md px-3 py-1.5 text-zinc-100 placeholder-zinc-600 outline-none focus:border-zinc-400 transition-colors text-sm"
          />
        </div>

        <!-- Scene groups -->
        <div v-for="group in groups" :key="group.sceneId" class="border-b border-zinc-800/40 last:border-0">

          <!-- Scene ID as section header -->
          <div class="px-3 pt-3.5 pb-1 flex items-center gap-2">
            <span class="w-0.5 h-4 rounded-full bg-zinc-600 flex-shrink-0"></span>
            <span class="text-xs font-semibold text-zinc-400 tracking-widest uppercase">
              {{ fromCamel(group.sceneId) }}
            </span>
          </div>

          <!-- Fields -->
          <div class="px-3 pb-3 flex flex-col gap-2.5">
            <div v-for="flatKey in group.keys" :key="flatKey">
              <!-- Field label -->
              <div class="text-xs text-zinc-600 tracking-widest uppercase mb-1">
                {{ fieldName(flatKey) }}
              </div>
              <!-- Field input -->
              <input
                  v-model="localValues[flatKey]"
                  @blur="commit(flatKey)"
                  @keydown.enter="($event.target as HTMLInputElement).blur()"
                  :placeholder="fieldName(flatKey)"
                  :class="[
                  'w-full bg-zinc-900 border rounded-md px-3 py-1.5 text-zinc-100 placeholder-zinc-700 outline-none transition-colors text-sm',
                  localValues[flatKey]
                    ? 'border-zinc-600 focus:border-zinc-400'
                    : 'border-zinc-800 focus:border-zinc-600'
                ]"
              />
            </div>
          </div>

        </div>
      </div>

    </template>
  </div>
</template>