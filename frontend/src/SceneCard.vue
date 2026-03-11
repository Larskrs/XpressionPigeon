<!-- SceneCard.vue -->
<script setup lang="ts">
import SceneInput from "@/SceneInput.vue"

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
</script>

<template>
  <div :class="['border rounded-lg overflow-hidden transition-all duration-200',
    isOn ? 'border-blue-500 shadow-lg shadow-blue-950' : 'border-zinc-800 hover:border-zinc-600']"
       class="bg-zinc-900">

    <!-- Header -->
    <div class="px-5 py-3 border-b border-zinc-800 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <span :class="['w-1.5 h-4 rounded-sm transition-colors duration-200', isOn ? 'bg-blue-400' : 'bg-zinc-600']"></span>
        <h3 class="text-lg font-semibold tracking-wide text-zinc-200">{{ fromCamel(sceneId) }}</h3>
      </div>
      <span v-if="isOn" class="text-[10px] tracking-widest uppercase text-blue-400 font-semibold">ON AIR</span>
    </div>

    <!-- Fields -->
    <div class="px-5 py-4 flex flex-col gap-3">
      <SceneInput
          v-for="(value, field) in fields"
          :key="field"
          :label="String(field)"
          :model-value="value"
          @commit="(val) => emit('update', sceneId, String(field), val)"
      />
    </div>

    <!-- Actions -->
    <div class="px-5 py-4 border-t border-zinc-800 flex gap-2">
      <button
          @click="emit('take', sceneId)"
          :class="['flex-1 flex items-center justify-center gap-2 text-xs font-semibold tracking-widest uppercase py-2.5 rounded transition-all duration-150 text-white',
            isOn ? 'bg-blue-500 cursor-default' : 'bg-blue-600 hover:bg-blue-500']"
      >
        ▶ {{ isOn ? 'ON AIR' : 'Take' }}
      </button>
      <button
          @click="emit('out', sceneId)"
          :class="['flex-1 px-4 py-2.5 rounded border text-xs font-semibold tracking-widest uppercase transition-all duration-150',
            !isOn ? 'bg-zinc-700 text-zinc-500 border-zinc-600 cursor-default' : 'bg-zinc-800 hover:bg-zinc-700 text-zinc-300 border-zinc-700']"
      >
        ◀ Out
      </button>
    </div>
  </div>
</template>