<!-- SceneCard.vue -->
<script setup lang="ts">
import SceneInput from "./scene-input.vue"
import { Icon} from "@iconify/vue";

const props = defineProps<{
  sceneId: string
  fields: Record<string, string>
  isOn: boolean
}>()

const emit = defineEmits<{
  take:   [sceneId: string]
  out:    [sceneId: string]
  update: [sceneId: string, key: string, value: string]
}>()

function fromCamel(str: string): string {
  return str.replace(/([A-Z])/g, " $1").trim()
}
function clearValues() {
  for (const [key, _] of Object.entries(props.fields)) {
    emit("update", props.sceneId, String(key), "")
  }
}
</script>

<template>
  <div :class="['rounded-lg overflow-hidden min-w-60 border transition-colors',
    isOn ? 'border-blue-500 bg-blue-800/5' : 'border-zinc-700']">

    <!-- Header -->
    <div class="flex items-center justify-between px-4 py-2.5 border-b border-zinc-800">
      <div class="flex items-center gap-2.5">
        <span :class="['w-2 h-2 rounded-full transition-colors', isOn ? 'bg-blue-400' : 'bg-zinc-600']"></span>
        <h3 class="text-sm font-medium text-zinc-100">{{ fromCamel(sceneId) }}</h3>
      </div>
      <span v-if="isOn" class="text-[10px] font-medium tracking-wide px-2 py-0.5 rounded bg-blue-950 text-blue-300">
        On air
      </span>
    </div>

    <!-- Fields -->
    <div class="px-4 py-3 flex flex-col gap-2">
      <SceneInput
          v-for="(value, field) in fields"
          :key="field"
          :label="String(field)"
          :model-value="value"
          @commit="(val: any) => emit('update', sceneId, String(field), val)"
      />
    </div>

    <!-- Actions -->
    <div class="px-4 py-3 border-t border-zinc-800 flex gap-2">
      <button class="flex justify-center items-center gap-2"
          @click="emit('take', sceneId)"
          :class="['flex-1 py-2 text-sm font-medium rounded transition-colors',
            isOn ? 'bg-blue-900 text-blue-300 cursor-default' : 'bg-blue-600 hover:bg-blue-500 text-white']"
      >
        <Icon class="size-4" icon="tabler:player-play-filled" />
        {{ isOn ? 'On air' : 'Take' }}
      </button>
      <button class="flex justify-center items-center gap-2"
          @click="emit('out', sceneId)"
          :class="['flex-1 py-2 text-sm font-medium rounded border transition-colors',
            isOn ? 'border-zinc-600 text-zinc-300 hover:bg-zinc-700' : 'border-zinc-700 text-zinc-600 cursor-default']"
      >
        <Icon class="size-5" icon="tabler:arrow-down"/> Out
      </button>
      <button class="max-w-8 flex justify-center items-center gap-2"
          @click="clearValues"
          :class="['flex-1 py-2 text-sm font-medium rounded border transition-colors',
            isOn ? 'border-zinc-600 text-zinc-300 hover:bg-zinc-700' : 'border-zinc-700 text-zinc-600 cursor-default']"
      >
        <Icon class="size-5" icon="tabler:trash"/>
      </button>
    </div>
  </div>
</template>