<script setup lang="ts">
import { computed } from "vue"

const props = defineProps<{
  modelValue: number | null
}>()

const emit = defineEmits<{
  (e: "update:modelValue", value: number | null): void
}>()

function normalize(val: unknown): number | null {
  if (val === "" || val === undefined) return null
  if (typeof val === "number") return val
  return null
}

function pad(n: number) {
  return String(n).padStart(2, "0")
}

function timestampToTime(ms: number | null) {
  if (!ms) return ""
  const d = new Date(ms)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function timeToTimestamp(str: string) {
  if (!str.trim()) return null

  const digits = str.replace(/\D/g, "").padEnd(6, "0").slice(0, 6)
  const hh = parseInt(digits.slice(0, 2)) || 0
  const nn = parseInt(digits.slice(2, 4)) || 0
  const ss = parseInt(digits.slice(4, 6)) || 0

  const base = new Date()
  base.setHours(hh, nn, ss, 0)

  return base.getTime()
}

const displayValue = computed({
  get() {
    return timestampToTime(normalize(props.modelValue))
  },
  set(val: string) {
    emit("update:modelValue", timeToTimestamp(val))
  }
})
</script>

<template>
  <input
      class="inputfield"
      type="text"
      inputmode="numeric"
      v-model="displayValue"
  />
</template>