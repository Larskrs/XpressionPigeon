<!-- SceneGroup.vue -->
<script setup lang="ts">
import { computed } from "vue"
import { Icon } from "@iconify/vue"
import SceneEditor from "@/dashboard/components/scene-editor.vue"
import type { SceneGroup } from "../useScenes.ts"

const props = defineProps<{
  group:       SceneGroup
  scenes:      Record<string, Record<string, string>>
  sceneStates: Record<string, boolean>
}>()

const emit = defineEmits<{
  take:   [sceneId: string]
  out:    [sceneId: string]
  update: [sceneId: string, key: string, value: string]
  flip:   [group: SceneGroup]
}>()

// Slice only the scenes this group cares about so SceneEditor props don't
// change when unrelated scenes update.
const groupScenes = computed(() => {
  const result: Record<string, Record<string, string>> = {}
  for (const id of props.group.sceneIds) {
    result[id] = props.scenes[id] ?? {}
  }
  return result
})

function clearAllValues() {
  for (const [sceneId, values] of Object.entries(groupScenes.value)) {
    for (const key of Object.keys(values)) {
      emit("update", sceneId, key, "")
    }
  }
}
</script>

<template>
  <div class="flex flex-col items-center gap-2">
    <div class="w-full flex items-center justify-between gap-4">
      <h2>{{ group.label }}</h2>
      <div class="flex flex-row gap-2">
        <button
            v-if="group.flip_enabled"
            class="cursor-pointer size-10 flex items-center justify-center rounded bg-blue-600 hover:bg-blue-500 text-white transition-colors flex-shrink-0"
            title="Take all"
            @click="group.sceneIds.forEach(sid => emit('take', sid))"
        >
          <Icon icon="tabler:player-play-filled" class="size-6" />
        </button>
        <button
            v-if="group.flip_enabled"
            class="cursor-pointer size-10 flex items-center justify-center rounded border border-red-900/50 bg-red-950/25 hover:bg-red-700 text-zinc-400 hover:text-zinc-200 transition-colors flex-shrink-0"
            title="Out all"
            @click="group.sceneIds.forEach(sid => emit('out', sid))"
        >
          <Icon icon="tabler:arrows-down" class="size-6" />
        </button>
        <button
            v-if="group.flip_enabled"
            class="cursor-pointer size-10 flex items-center justify-center rounded border border-zinc-700 bg-zinc-800 hover:bg-zinc-700 text-zinc-400 hover:text-zinc-200 transition-colors flex-shrink-0"
            title="Swap fields"
            @click="emit('flip', group)"
        >
          <Icon icon="tabler:arrows-right-left" class="size-6" />
        </button>
        <button
            class="cursor-pointer size-10 flex items-center justify-center rounded border border-zinc-700 bg-zinc-800 hover:bg-zinc-700 text-zinc-400 hover:text-zinc-200 transition-colors flex-shrink-0"
            title="Swap fields"
            @click="clearAllValues"
        >
          <Icon icon="tabler:trash" class="size-6" />
        </button>
      </div>
    </div>
    <div class="flex items-center gap-2">
      <SceneEditor
          v-for="sceneId in group.sceneIds"
          :key="sceneId"
          :scene-id="sceneId"
          :fields="groupScenes[sceneId]"
          :is-on="sceneStates[sceneId] ?? false"
          @take="emit('take', $event)"
          @out="emit('out', $event)"
          @update="(sceneId: string, key: string, val: string) => emit('update', sceneId, key, val)"
      />
    </div>
  </div>
</template>