<!-- SceneInput.vue -->
<script setup lang="ts">
import {computed, ref, watch} from "vue"

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
const isFocused  = ref(false)

const isFloated = computed(() => isFocused.value || localValue.value !== "")

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
  <div class="relative mt-0">
    <!-- Floating label -->
    <label
        class="absolute left-3 transition-all tracking-widest duration-250 ease-in-out pointer-events-none z-10 origin-left"
        :class="isFloated
          ? 'top-0 -translate-y-1/2 text-zinc-500 text-xs bg-zinc-950 px-1.5'
          : 'top-[0.6rem] translate-y-0 text-sm text-zinc-600 bg-transparent px-0'"
    >{{ label }}</label>

    <textarea
        v-model="localValue"
        rows="1"
        @focus="isFocused = true"
        @blur="isFocused = false; commit()"
        @keydown="onKeydown"
        class="w-full bg-zinc-950 text-zinc-100 text-[12px] rounded px-3 py-2 resize-none leading-relaxed border border-zinc-700 hover:border-zinc-600 focus:border-zinc-500 focus:outline-none transition-colors"
        style="field-sizing: content; min-height: 2.25rem;"
    />
  </div>
</template>