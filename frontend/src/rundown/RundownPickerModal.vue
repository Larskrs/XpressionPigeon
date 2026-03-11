<!-- RundownPickerModal.vue -->
<script setup lang="ts">
import { ref, computed } from "vue"
import Modal from "../components/Modal.vue"

const props = defineProps<{
  modelValue: boolean
  rundownList: { id: string; name: string }[]
  currentId: string | null
}>()

const emit = defineEmits<{
  "update:modelValue": [value: boolean]
  load:   [id: string]
  create: [name: string]
  rename: [id: string, newName: string]
  delete: [id: string]
}>()

const newName     = ref("")
const renamingId  = ref<string | null>(null)
const renameValue = ref("")
const confirmDeleteId = ref<string | null>(null)
const search      = ref("")

const filteredList = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return props.rundownList
  return props.rundownList.filter(r => r.name.toLowerCase().includes(q))
})

function submitCreate() {
  if (!newName.value.trim()) return
  emit("create", newName.value.trim())
  newName.value = ""
}

function startRename(id: string, currentName: string) {
  renamingId.value  = id
  renameValue.value = currentName
}

function submitRename(id: string) {
  if (renameValue.value.trim() && renameValue.value.trim() !== id) {
    emit("rename", id, renameValue.value.trim())
  }
  renamingId.value = null
}

function confirmDelete(id: string) {
  confirmDeleteId.value = id
}

function executeDelete() {
  if (confirmDeleteId.value) {
    emit("delete", confirmDeleteId.value)
    confirmDeleteId.value = null
  }
}
</script>

<template>
  <Modal
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
      title="Rundowns"
      size="md"
      :show-close="true"
  >
    <div class="flex flex-col gap-5 font-mono">

      <!-- Existing rundowns -->
      <div>
        <p class="text-xs text-zinc-500 tracking-widest uppercase mb-3">Saved rundowns</p>

        <!-- Search -->
        <input
            v-model="search"
            placeholder="Search…"
            class="w-full bg-zinc-900 border border-zinc-700 rounded-md px-3 py-2 text-sm text-zinc-200 placeholder-zinc-600 outline-none focus:border-zinc-500 transition-colors font-mono mb-3"
        />

        <div v-if="filteredList.length" class="flex flex-col gap-1.5">
          <div
              v-for="r in filteredList" :key="r.id"
              class="flex items-center gap-2 px-3 py-2.5 rounded-lg border border-zinc-800 bg-zinc-900 group transition-colors"
              :class="r.id === currentId ? 'border-blue-600/40' : 'hover:border-zinc-700'"
          >
            <!-- Rename input -->
            <template v-if="renamingId === r.id">
              <input
                  v-model="renameValue"
                  @keydown.enter="submitRename(r.id)"
                  @keydown.escape="renamingId = null"
                  autofocus
                  class="flex-1 bg-transparent border-b border-amber-500 text-zinc-200 text-sm outline-none py-0.5 font-mono"
              />
              <button @click="submitRename(r.id)"
                      class="text-xs text-amber-400 hover:text-amber-300 tracking-widest uppercase transition-colors px-2">
                Save
              </button>
              <button @click="renamingId = null"
                      class="text-xs text-zinc-600 hover:text-zinc-400 transition-colors px-1">
                ✕
              </button>
            </template>

            <!-- Confirm delete -->
            <template v-else-if="confirmDeleteId === r.id">
              <span class="flex-1 text-sm text-red-400">Delete "{{ r.name }}"?</span>
              <button @click="executeDelete"
                      class="text-xs text-red-400 hover:text-red-300 tracking-widest uppercase px-2 transition-colors">
                Confirm
              </button>
              <button @click="confirmDeleteId = null"
                      class="text-xs text-zinc-600 hover:text-zinc-400 px-1 transition-colors">
                ✕
              </button>
            </template>

            <!-- Normal row -->
            <template v-else>
              <button @click="emit('load', r.id)"
                      class="flex-1 text-left text-sm transition-colors"
                      :class="r.id === currentId ? 'text-blue-300' : 'text-zinc-200 hover:text-white'"
              >
                {{ r.name }}
                <span v-if="r.id === currentId" class="ml-2 text-xs text-blue-500">● loaded</span>
              </button>
              <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button @click="startRename(r.id, r.name)"
                        class="w-7 h-7 flex items-center justify-center rounded text-zinc-600 hover:text-zinc-300 hover:bg-zinc-800 transition-colors text-xs"
                        title="Rename">✎</button>
                <button @click="confirmDelete(r.id)"
                        class="w-7 h-7 flex items-center justify-center rounded text-zinc-600 hover:text-red-400 hover:bg-zinc-800 transition-colors text-xs"
                        title="Delete">✕</button>
              </div>
            </template>
          </div>
        </div>

        <div v-else class="flex items-center justify-center h-16 text-xs text-zinc-700 tracking-widest uppercase border border-dashed border-zinc-800 rounded-lg">
          {{ search.trim() ? "No matches" : "No saved rundowns" }}
        </div>
      </div>

      <div class="border-t border-zinc-800"></div>

      <!-- Create new -->
      <div class="flex flex-col gap-2">
        <p class="text-xs text-zinc-500 tracking-widest uppercase">New rundown</p>
        <div class="flex gap-2">
          <input
              v-model="newName"
              @keydown.enter="submitCreate"
              placeholder="Rundown name…"
              class="flex-1 bg-zinc-900 border border-zinc-700 rounded-md px-3 py-2 text-sm text-zinc-200 placeholder-zinc-600 outline-none focus:border-zinc-500 transition-colors font-mono"
          />
          <button
              @click="submitCreate"
              :disabled="!newName.trim()"
              class="px-4 py-2 rounded-md bg-blue-600 hover:bg-blue-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs font-semibold tracking-widest uppercase transition-colors"
          >
            Create
          </button>
        </div>
      </div>

    </div>
  </Modal>
</template>