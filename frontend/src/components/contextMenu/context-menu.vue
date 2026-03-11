<script setup lang="ts">
import { onMounted, onBeforeUnmount } from "vue"
import { useContextMenu } from "./useContextMenu"
import { Icon } from "@iconify/vue"

const { state, close } = useContextMenu()

function onGlobalClick() {
  close()
}

function onKey(e: KeyboardEvent) {
  if (e.key === "Escape") close()
}

onMounted(() => {
  document.addEventListener("click", onGlobalClick)
  window.addEventListener("resize", close)
  document.addEventListener("scroll", close, true)
  document.addEventListener("keydown", onKey)
})

onBeforeUnmount(() => {
  document.removeEventListener("click", onGlobalClick)
  window.removeEventListener("resize", close)
  document.removeEventListener("scroll", close, true)
  document.removeEventListener("keydown", onKey)
})
</script>

<template>
  <Teleport to="body">
    <div
        v-if="state.visible"
        ref="menuRef"
        class="context-menu rounded bg-surface shadow-md rounded-md overflow-hidden"
        :style="{
    position: 'fixed',
    left: state.x + 'px',
    top: state.y + 'px'
  }"
    >
      <template v-for="(item, i) in state.items" :key="i">

        <hr class="my-2" v-if="item.type === 'separator'" />

        <button
            v-else
            class="px-3 py-1 hover:bg-active hover:text-bg cursor-pointer w-full flex items-center gap-2 text-start text-text"
            :class="{ danger: item.danger }"
            :disabled="item.disabled"
            @click.stop="
    !item.disabled && item.action?.();
    close();
  "
        >
          <Icon
              v-if="item.icon"
              :icon="item.icon"
              class="w-4 h-4"
          />

          <span class="flex-1 text-sm">
            {{ item.label }}
          </span>
        </button>

      </template>
    </div>
  </Teleport>
</template>