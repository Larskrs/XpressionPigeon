<script setup lang="ts">
defineProps<{
  modelValue: boolean | null | undefined
}>()

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void
}>()

function onInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target) return

  emit("update:modelValue", Boolean(target.value))
}
</script>

<template>
  <input
      class="inputfield"
      type="checkbox"
      :checked="!!modelValue"
      @change="onInput"
  />
</template>