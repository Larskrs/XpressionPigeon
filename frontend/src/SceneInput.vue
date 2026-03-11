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
const isDirty = ref(false)
const isWaiting = ref(false)
const justCommitted = ref(false)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(() => props.modelValue, (val) => {
  if (!isFocused.value) localValue.value = val
})

function onInput() {
  isDirty.value = true
  isWaiting.value = true
  justCommitted.value = false
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { if (isDirty.value) commit() }, 1000)
}

function onBlur() {
  isFocused.value = false
  if (isDirty.value) commit()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault()
    commit()
    textareaRef.value?.blur()
  }
}

function commit() {
  if (debounceTimer) { clearTimeout(debounceTimer); debounceTimer = null }
  isDirty.value = false
  isWaiting.value = false
  justCommitted.value = true
  emit("update:modelValue", localValue.value)
  emit("commit", localValue.value)
  setTimeout(() => { justCommitted.value = false }, 1500)
}
</script>

<template>
  <div class="flex flex-col gap-1">

    <!-- Label + status -->
    <div class="flex items-center justify-between">
      <label class="text-[10px] tracking-widest uppercase text-zinc-500">{{ label }}</label>
      <transition name="fade">
        <span v-if="isWaiting" class="flex items-center gap-1 text-[9px] text-amber-400">
          <svg class="w-2.5 h-2.5 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
          </svg>
        </span>
        <span v-else-if="justCommitted" class="text-[9px] text-emerald-400">✓</span>
      </transition>
    </div>

    <!-- Input -->
    <textarea
        ref="textareaRef"
        v-model="localValue"
        :placeholder="placeholder ?? label"
        rows="1"
        @input="onInput"
        @focus="isFocused = true"
        @blur="onBlur"
        @keydown="onKeydown"
        class="w-full bg-zinc-800/60 text-zinc-100 text-sm rounded px-3 py-2 placeholder-zinc-600 resize-none leading-relaxed border focus:outline-none transition-colors duration-150"
        :class="isWaiting    ? 'border-amber-500/50' :
                justCommitted ? 'border-emerald-500/40' :
                isFocused     ? 'border-zinc-500' :
                                'border-zinc-700 hover:border-zinc-600'"
        style="field-sizing: content; min-height: 2.25rem;"
    />

  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>