<script setup lang="ts">
import { ref, watch } from "vue"

const props = defineProps<{
  modelValue: number | null
}>()

const emit = defineEmits<{
  (e: "update:modelValue", value: number): void
}>()

function pad(n: number) {
  return String(n).padStart(2, "0")
}

function millisToTime(ms: number | null) {
  const totalSeconds = Math.floor((ms ?? 0) / 1000)
  const hh = Math.floor(totalSeconds / 3600)
  const nn = Math.floor((totalSeconds % 3600) / 60)
  const ss = totalSeconds % 60
  return `${pad(hh)}:${pad(nn)}:${pad(ss)}`
}

function timeToMillis(str: string) {
  const digits = str.replace(/\D/g, "").padEnd(6, "0").slice(0, 6)
  const hh = Math.min(23, parseInt(digits.slice(0, 2)) || 0)
  const nn = Math.min(59, parseInt(digits.slice(2, 4)) || 0)
  const ss = Math.min(59, parseInt(digits.slice(4, 6)) || 0)
  return ((hh * 60 + nn) * 60 + ss) * 1000
}

function formatNow() {
  internalValue.value = millisToTime(timeToMillis(internalValue.value))
}

const internalValue = ref(millisToTime(props.modelValue))

watch(
    () => props.modelValue,
    (newVal) => {
      internalValue.value = millisToTime(newVal)
    }
)

function commit() {
  emit("update:modelValue", timeToMillis(internalValue.value))
}

function revert() {
  internalValue.value = millisToTime(props.modelValue)
}
</script>

<template>
  <input
      type="text"
      inputmode="numeric"
      :value="internalValue"
      @input="internalValue = ($event.target as HTMLInputElement).value"
      @keydown.enter.prevent="commit"
      @keydown.esc.prevent="revert"
      @blur="formatNow"
      @focus="($event.target as HTMLInputElement).select()"
      @focusout="commit"
      placeholder="HH:MM:SS"
      class="inputfield time-input max-w-25"
  />
</template>
