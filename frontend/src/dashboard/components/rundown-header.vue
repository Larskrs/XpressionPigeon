<script setup lang="ts">
import { ref } from "vue"

const props = defineProps<{ name: string; pageCount: number }>()
const emit  = defineEmits<{ save: []; rename: [name: string] }>()

const editing   = ref(false)
const nameInput = ref(props.name)

function commit() {
  editing.value = false
  if (nameInput.value.trim() && nameInput.value !== props.name)
    emit("rename", nameInput.value.trim())
}
</script>

<template>
  <div class="flex items-center gap-3 px-5 py-3 border-b border-zinc-800 bg-zinc-900 shrink-0 font-mono text-sm">
    <template v-if="editing">
      <input
          v-model="nameInput"
          class="flex-1 bg-zinc-800 text-zinc-100 px-2 py-1 rounded border border-zinc-600 focus:outline-none focus:border-zinc-400"
          autofocus
          @blur="commit"
          @keydown.enter="commit"
          @keydown.esc="editing = false"
      />
    </template>
    <template v-else>
      <span
          class="flex-1 text-zinc-200 cursor-pointer hover:text-white transition-colors"
          title="Double-click to rename"
          @dblclick="editing = true"
      >{{ name }}</span>
    </template>

    <span class="text-xs text-zinc-600 tabular-nums shrink-0">{{ pageCount }}p</span>

    <button
        class="text-xs px-3 py-1 rounded bg-zinc-700 hover:bg-zinc-600 text-zinc-300 hover:text-white transition-colors shrink-0"
        @click="emit('save')"
    >Save</button>
  </div>
</template>