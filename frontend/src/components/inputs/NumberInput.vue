<script setup lang="ts">
defineProps<{
  modelValue: number | null | undefined
}>()

const emit = defineEmits<{
  (e: "update:modelValue", value: number): void
}>()

function onInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target) return

  emit("update:modelValue", Number(target.value))
}
</script>

<template>
  <input
      class="inputfield"
      type="number"
      :value="modelValue ?? 0"
      @input="onInput"
  />
</template>