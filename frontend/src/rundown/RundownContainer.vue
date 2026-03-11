<!-- RundownContainer.vue -->
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue"
import { useRundown } from "./useRundown.ts"
import RundownTable from "./RundownTable.vue"
import RundownEditPanel from "./RundownEditPanel.vue"
import RundownPickerModal from "./RundownPickerModal.vue"
import type { WebSocketManager } from "../shared/network/WebSocketManager"

const props = defineProps<{
  socket: WebSocketManager<any>
}>()

const emit = defineEmits<{
  take:   [sceneId: string]
  out:    [sceneId: string]
  update: [sceneId: string, key: string, value: string]
}>()

// ─── Composable ───────────────────────────────────────────────────────────────
const rd = useRundown(props.socket)
const pickerOpen = ref(false)

// ─── Socket listener ──────────────────────────────────────────────────────────
onMounted(() => {
  props.socket.on("message", rd.onServerEvent)
  window.addEventListener("keydown", onKeyDown)
})
onBeforeUnmount(() => {
  props.socket.off("message", rd.onServerEvent)
  window.removeEventListener("keydown", onKeyDown)
})

// ─── Picker handlers ──────────────────────────────────────────────────────────
function openPicker() {
  rd.requestList()
  pickerOpen.value = true
}

// ─── Row selection + XPression events ─────────────────────────────────────────
function selectRow(idx: number) {
  rd.selectedIdx.value = idx
  rd.clearBulkSelect()

  if (rd.mode.value === "editor") return

  const row = rd.rows.value[idx]
  for (const flatKey of rd.dataHeaders.value) {
    const u = flatKey.lastIndexOf("_")
    emit("update", flatKey.substring(0, u), flatKey.substring(u + 1), row?.[flatKey] ?? "")
  }

  scrollActiveRowIntoView()
}

function takeOrOutCurrentRow() {
  if (rd.mode.value === "editor" || rd.selectedIdx.value === null) return
  const row = rd.rows.value[rd.selectedIdx.value] ?? {}
  rd.sceneIdsForRow(row).forEach(({ sceneId, hasContent }) => {
    if (hasContent) emit("take", sceneId)
    else emit("out", sceneId)
  })
}

function outPreviousRow() {
  if (rd.mode.value === "editor" || rd.selectedIdx.value === null) return
  const row = rd.rows.value[rd.selectedIdx.value] ?? {}
  rd.sceneIdsForRow(row).forEach(({ sceneId, hasContent }) => {
    if (hasContent) emit("out", sceneId)
  })
}

// ─── Keyboard ─────────────────────────────────────────────────────────────────
function onKeyDown(e: KeyboardEvent) {
  if ((e.target as HTMLElement).tagName === "INPUT") return
  if (!rd.rows.value.length) return

  if (e.key === "ArrowDown" || e.key === "PageDown") {
    e.preventDefault()
    const next = rd.selectedIdx.value === null ? 0 : Math.min(rd.selectedIdx.value + 1, rd.rows.value.length - 1)
    selectRow(next)
  } else if (e.key === "ArrowUp" || e.key === "PageUp") {
    e.preventDefault()
    const prev = rd.selectedIdx.value === null ? 0 : Math.max(rd.selectedIdx.value - 1, 0)
    selectRow(prev)
  } else if (e.key === "," || e.key === "Home") {
    e.preventDefault()
    takeOrOutCurrentRow()
  } else if (e.key === "." || e.key === "End") {
    e.preventDefault()
    outPreviousRow()
  }
}

function scrollActiveRowIntoView() {
  setTimeout(() => {
    document.querySelector("tr.active-row")?.scrollIntoView({ block: "nearest", behavior: "smooth" })
  }, 0)
}

// ─── Edit panel handlers ──────────────────────────────────────────────────────
function onPanelUpdate(flatKey: string, value: string) {
  if (rd.selectedIdx.value === null) return
  rd.updateRowField(rd.selectedIdx.value, flatKey, value)
  // Also push to XPression live if in production mode
  if (rd.mode.value === "production") {
    const u = flatKey.lastIndexOf("_")
    emit("update", flatKey.substring(0, u), flatKey.substring(u + 1), value)
  }
}
</script>

<template>
  <div class="flex flex-col gap-3 font-mono h-full">

    <!-- Toolbar -->
    <div class="flex items-center gap-3 flex-wrap">

      <!-- Rundown picker button -->
      <button @click="openPicker"
              class="inline-flex items-center gap-2 px-4 py-2 rounded-md border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-xs text-zinc-300 tracking-widest uppercase transition-colors">
        <svg class="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h18M3 12h18M3 17h18"/>
        </svg>
        {{ rd.currentName.value || "Select Rundown" }}
      </button>

      <!-- Dirty + save -->
      <template v-if="rd.currentName.value">
        <span v-if="rd.isDirty.value" class="text-xs text-amber-500/70">● unsaved</span>
        <button @click="rd.saveRundown"
                :class="['inline-flex items-center gap-1.5 px-3 py-2 rounded-md border text-xs font-semibold tracking-widest uppercase transition-colors',
            rd.isDirty.value
              ? 'border-amber-600/50 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400'
              : 'border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-zinc-500']">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17 21H7a2 2 0 01-2-2V5a2 2 0 012-2h7l5 5v11a2 2 0 01-2 2zM17 21V13H7v8M7 3v5h8"/>
          </svg>
          Save
        </button>
      </template>

      <span v-if="rd.rows.value.length" class="text-xs text-zinc-600">
        {{ rd.rows.value.length }} row{{ rd.rows.value.length !== 1 ? "s" : "" }}
      </span>

      <!-- Mode toggle -->
      <div class="flex rounded-md overflow-hidden border border-zinc-700 text-xs font-semibold tracking-widest uppercase">
        <button @click="rd.mode.value = 'production'"
                :class="['px-4 py-2 transition-colors', rd.mode.value === 'production' ? 'bg-blue-600 text-white' : 'bg-zinc-900 text-zinc-500 hover:bg-zinc-800']">
          ▶ Production
        </button>
        <div class="w-px bg-zinc-700"></div>
        <button @click="rd.mode.value = 'editor'"
                :class="['px-4 py-2 transition-colors', rd.mode.value === 'editor' ? 'bg-amber-500 text-zinc-950' : 'bg-zinc-900 text-zinc-500 hover:bg-zinc-800']">
          ✎ Editor
        </button>
      </div>

      <!-- Editor actions -->
      <template v-if="rd.mode.value === 'editor' && rd.currentName.value">
        <button @click="rd.appendFromStore"
                class="px-3 py-2 rounded-md border border-amber-600/40 bg-amber-500/10 hover:bg-amber-500/20 text-amber-400 text-xs font-semibold tracking-widest uppercase transition-colors">
          + Append live values
        </button>
        <button @click="rd.insertRowBelow"
                class="px-3 py-2 rounded-md border border-zinc-700 bg-zinc-900 hover:bg-zinc-800 text-zinc-400 text-xs font-semibold tracking-widest uppercase transition-colors">
          + Add row
        </button>
      </template>

      <!-- Keyboard hints (production) -->
      <span v-if="rd.rows.value.length && rd.mode.value === 'production'" class="ml-auto text-xs text-zinc-700 flex items-center gap-3">
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">↑↓</kbd> navigate</span>
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">,</kbd> take/out</span>
        <span><kbd class="px-1.5 py-0.5 rounded border border-zinc-700 bg-zinc-900 text-zinc-500">.</kbd> out prev</span>
      </span>
      <span v-if="rd.mode.value === 'editor'" class="ml-auto text-xs text-amber-600/60">
        ⇧ + click to multi-select · drag to reorder
      </span>
    </div>

    <!-- Split: table + edit panel -->
    <div class="flex gap-3 min-h-0 flex-1">

      <!-- Table (left, grows) -->
      <div class="flex-1 min-w-0">
        <RundownTable
            :rows="rd.rows.value"
            :data-headers="rd.dataHeaders.value"
            :label-header="rd.labelHeader.value"
            :selected-idx="rd.selectedIdx.value"
            :selected-set="rd.selectedSet.value"
            :mode="rd.mode.value"
            @select-row="selectRow"
            @toggle-bulk-select="rd.toggleBulkSelect"
            @move-row="rd.moveRow"
            @insert-above="(idx: number) => rd.insertRowAt(idx)"
            @insert-below="(idx: number) => rd.insertRowAt(idx + 1)"
            @delete-row="rd.deleteRow"
            @bulk-delete="rd.bulkDelete"
        />
      </div>

      <!-- Edit panel (right, fixed width, editor mode only) -->
      <Transition
          enter-active-class="transition-all duration-200"
          leave-active-class="transition-all duration-150"
          enter-from-class="opacity-0 translate-x-4"
          leave-to-class="opacity-0 translate-x-4"
      >
        <div v-if="rd.mode.value === 'editor'"
             class="w-72 flex-shrink-0 rounded-lg border border-zinc-800 bg-zinc-950 overflow-hidden flex flex-col">
          <RundownEditPanel
              :row="rd.selectedIdx.value !== null ? (rd.rows.value[rd.selectedIdx.value] ?? null) : null"
              :row-index="rd.selectedIdx.value"
              :data-headers="rd.dataHeaders.value"
              :label-header="rd.labelHeader.value"
              @update="onPanelUpdate"
              @insert-above="rd.insertRowAbove"
              @insert-below="rd.insertRowBelow"
              @delete="() => rd.selectedIdx.value !== null && rd.deleteRow(rd.selectedIdx.value)"
              @replace-from-store="() => rd.selectedIdx.value !== null && rd.replaceRowFromStore(rd.selectedIdx.value)"
          />
        </div>
      </Transition>

    </div>

    <!-- Empty state (no rundown loaded) -->
    <div v-if="!rd.currentName.value"
         class="flex flex-col items-center justify-center h-32 border border-dashed border-zinc-800 rounded-lg text-zinc-700 gap-2">
      <span class="text-xs tracking-widest uppercase">No rundown loaded</span>
      <button @click="openPicker" class="text-xs text-zinc-600 hover:text-zinc-400 underline underline-offset-2 transition-colors">
        Open rundown picker
      </button>
    </div>

    <!-- Picker modal -->
    <RundownPickerModal
        v-model="pickerOpen"
        :rundown-list="rd.rundownList.value"
        :current-id="rd.currentId.value"
        @load="(id: string) => { rd.requestLoad(id); pickerOpen = false }"
        @create="(name: string) => { rd.createNewRundown(name); pickerOpen = false }"
        @rename="rd.renameRundown"
        @delete="rd.deleteRundown"
    />

  </div>
</template>