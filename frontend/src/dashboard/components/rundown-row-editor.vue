<script setup lang="ts">
import { ref, watch, computed } from "vue"
import { toRaw } from "vue"
import type { Row } from "../useRundown.ts"
import { Icon } from "@iconify/vue"
import SceneInput from "@/dashboard/components/scene-input.vue";

const props = defineProps<{
  row:    Row
  scenes: Record<string, Record<string, string>>
}>()

const emit = defineEmits<{
  save:   [row: Row]
  cancel: []
  take:   [row: Row]
  outAll: []
}>()

function cloneRow(row: Row): Row {
  return JSON.parse(JSON.stringify(toRaw(row)))
}

const draft = ref<Row>(cloneRow(props.row))

watch(() => props.row.id, () => {
  draft.value = cloneRow(props.row)
  openScenes.value = new Set(Object.keys(props.scenes).filter(sceneId =>
      Object.keys(props.scenes[sceneId]!).some(field =>
          getVal(`${sceneId}.${field}`) !== ""
      )
  ))
})

function getVal(key: string): string {
  return draft.value.values.find(p => p.name === key)?.value ?? ""
}

function setVal(key: string, value: string) {
  const existing = draft.value.values.find(p => p.name === key)
  if (existing) existing.value = value
  else          draft.value.values.push({ name: key, value })
}

const sceneGroups = computed(() =>
    Object.entries(props.scenes).map(([sceneId, fields]) => ({
      sceneId,
      fields: Object.keys(fields).map(field => ({ field, key: `${sceneId}.${field}` })),
    }))
)


const openScenes = ref<Set<string>>(
    new Set(Object.keys(props.scenes).filter(sceneId =>
        Object.keys(props.scenes[sceneId]!).some(field =>
            getVal(`${sceneId}.${field}`) !== ""
        )
    ))
)

function toggleScene(id: string) {
  openScenes.value.has(id) ? openScenes.value.delete(id) : openScenes.value.add(id)
}

function save() {
  draft.value.values = draft.value.values.filter(p => p.value !== "")
  emit("save", cloneRow(draft.value))
}
</script>

<template>
  <aside class="flex flex-col w-full shrink-0 rounded-lg border border-zinc-800 bg-zinc-950 overflow-hidden h-full">

    <!-- Header -->
    <header class="flex items-center gap-2 px-3 py-2.5 border-b border-zinc-800 bg-zinc-900 shrink-0">
      <span class="flex-1 text-sm font-medium text-zinc-200 truncate">
        {{ draft.name || "Unnamed row" }}
      </span>
      <button
          class="p-1 rounded text-zinc-500 hover:text-zinc-200 hover:bg-zinc-700 transition-colors"
          title="Close"
          @click="emit('cancel')"
      >
        <Icon icon="tabler:x" class="size-4" />
      </button>
    </header>

    <!-- Scene sticky toolbar -->
    <div class="flex items-center gap-3 px-3 py-2 border-b border-zinc-800/60 bg-zinc-950 shrink-0">
      <span class="flex-1 text-xs text-zinc-500 uppercase tracking-wider">Scenes</span>
      <button
          class="text-xs text-zinc-600 hover:text-zinc-300 transition-colors"
          @click="openScenes = new Set(Object.keys(scenes))"
      >Expand all</button>
      <button
          class="text-xs text-zinc-600 hover:text-zinc-300 transition-colors"
          @click="openScenes.clear()"
      >Collapse all</button>
    </div>

    <!-- Scene list — only this scrolls -->
    <div class="flex-1 overflow-y-auto min-h-0">
      <div
          v-for="{ sceneId, fields } in sceneGroups"
          :key="sceneId"
          class="border-b border-zinc-800/40 last:border-b-0"
      >
        <button
            class="w-full flex items-center gap-2 px-3 py-2 hover:bg-zinc-800/50 transition-colors text-left"
            @click="toggleScene(sceneId)"
        >
          <svg
              class="w-3 h-3 shrink-0 text-zinc-600 transition-transform duration-150"
              :class="{ 'rotate-90': openScenes.has(sceneId) }"
              viewBox="0 0 12 12" fill="none"
          >
            <path d="M4 2l4 4-4 4" stroke="currentColor" stroke-width="1.5"
                  stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="flex-1 text-sm text-zinc-300 font-mono">{{ sceneId }}</span>
          <span
              v-if="fields.some(f => getVal(f.key) !== '')"
              class="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0"
              title="Has saved values"
          />
        </button>

        <div v-if="openScenes.has(sceneId)" class="px-3 pb-3 pt-2 space-y-3">
          <SceneInput
              v-for="{ field, key } in fields"
              :key="key"
              :label="field"
              :model-value="getVal(key)"
              @update:model-value="setVal(key, $event)"
              @commit="setVal(key, $event)"
          />
        </div>
      </div>

      <div v-if="sceneGroups.length === 0" class="px-3 py-8 text-xs text-zinc-600 text-center">
        No scenes available
      </div>
    </div>

    <!-- Footer — always visible -->
    <footer class="flex gap-2 px-3 py-2.5 border-t border-zinc-800 shrink-0 bg-zinc-900">
      <button
          class="flex-1 bg-emerald-700 hover:bg-emerald-600 active:bg-emerald-800 rounded px-3 py-1.5 text-sm text-white font-medium transition-colors"
          @click="save"
      >Save</button>
      <button
          class="bg-zinc-700 hover:bg-zinc-600 rounded px-3 py-1.5 text-sm text-zinc-300 transition-colors"
          @click="emit('cancel')"
      >Cancel</button>
    </footer>

    <footer class="grid grid-cols-2 gap-2 px-3 py-2.5 border-t border-zinc-800 shrink-0 bg-zinc-900">
      <button
          class="bg-blue-600 hover:bg-blue-500 rounded px-3 py-1.5 text-sm text-zinc-300 transition-colors"
          @click="emit('take', draft)"
      >Take</button>

      <button
          class="bg-zinc-700 hover:bg-zinc-600 rounded px-3 py-1.5 text-sm text-zinc-300 transition-colors"
          @click="emit('outAll')"
      >Out</button>
    </footer>
  </aside>
</template>