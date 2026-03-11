<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from "vue"

const props = defineProps<{
  modelValue: string | null | undefined
}>()

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void
}>()

const editor = ref<HTMLSpanElement | null>(null)

function onInput() {
  if (!editor.value) return
  emit("update:modelValue", editor.value.innerText)
}

onMounted(() => {
  if (editor.value) {
    editor.value.innerText = props.modelValue ?? ""
  }
})

// Sync external modelValue changes into the DOM without moving the cursor
watch(
  () => props.modelValue,
  (val) => {
    nextTick(() => {
      if (!editor.value) return
      const incoming = val ?? ""
      if (editor.value.innerText !== incoming) {
        editor.value.innerText = incoming
      }
    })
  },
)
</script>

<template>
  <div
    ref="editor"
    class="inputfield min-w-8 h-full break-words whitespace-pre-wrap"
    contenteditable="true"
    role="textbox"
    aria-multiline="true"
    @input="onInput"
  />
</template>