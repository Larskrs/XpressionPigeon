<!-- SceneInput.vue -->
<script setup lang="ts">
import { ref, watch } from "vue"

const props = defineProps<{
  label: string
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  "commit": [value: string]
}>()

const localValue = ref(props.modelValue)
const isFocused = ref(false)

watch(() => props.modelValue, (val) => {
  if (!isFocused.value) localValue.value = val
})

function commit() {
  emit("update:modelValue", localValue.value)
  emit("commit", localValue.value)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault()
    commit()
    ;(e.target as HTMLTextAreaElement).blur()
  }
}
</script>

<template>
  <div class="flex flex-col gap-1">
    <label class="text-[10px] tracking-widest uppercase text-zinc-500">{{ label }}</label>
    <textarea
        v-model="localValue"
        :placeholder="placeholder ?? label"
        rows="1"
        @focus="isFocused = true"
        @blur="isFocused = false; commit()"
        @keydown="onKeydown"
        class="w-full bg-zinc-800/60 text-zinc-100 text-sm rounded px-3 py-2 placeholder-zinc-600 resize-none leading-relaxed border border-zinc-700 hover:border-zinc-600 focus:border-zinc-500 focus:outline-none transition-colors"
        style="field-sizing: content; min-height: 2.25rem;"
    />
  </div>
</template>