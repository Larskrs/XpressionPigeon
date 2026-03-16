<template>
  <Modal
      :id="MODAL_ID"
      title="Placeholders"
      size="md"
      backdrop="blur"
  >
    <div class="flex flex-col gap-4">

      <!-- Existing placeholders -->
      <div v-if="placeholders.length > 0" class="flex flex-col gap-1">
        <TransitionGroup
            enter-active-class="transition-all duration-200 ease-out"
            leave-active-class="transition-all duration-150 ease-in absolute w-full"
            enter-from-class="opacity-0 -translate-y-1"
            leave-to-class="opacity-0 translate-y-1"
        >
          <div
              v-for="ph in placeholders"
              :key="ph.key"
              class="group flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-muted/40 hover:bg-muted transition-colors"
          >
            <!-- Key badge -->
            <span class="font-mono text-xs font-medium text-primary bg-primary/10 px-2 py-0.5 rounded shrink-0 select-all">
              %{{ ph.key }}%
            </span>

            <!-- Value input -->
            <input
                :value="ph.value"
                @change="e => updateValue(ph.key, (e.target as HTMLInputElement).value)"
                @keydown.enter="e => (e.target as HTMLInputElement).blur()"
                class="flex-1 min-w-0 bg-transparent text-sm text-text outline-none border-none placeholder:text-text-muted"
                :placeholder="`value for %${ph.key}%`"
                spellcheck="false"
            />

            <!-- Delete -->
            <button
                @click="remove(ph.key)"
                class="opacity-0 group-hover:opacity-100 flex items-center justify-center w-6 h-6 rounded text-text-muted hover:text-danger hover:bg-danger/10 transition-all shrink-0"
                :aria-label="`Remove ${ph.key}`"
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </TransitionGroup>
      </div>

      <!-- Empty state -->
      <p v-else class="text-sm text-text-muted text-center py-4">
        No placeholders yet. Add one below.
      </p>

      <!-- Add new -->
      <div class="flex gap-2 pt-1 border-t border-border">
        <input
            v-model="newKey"
            @keydown.enter="focusValue"
            ref="keyInputRef"
            class="w-36 shrink-0 px-3 py-1.5 rounded-lg text-sm font-mono border border-border bg-surface text-text outline-none focus:border-primary transition-colors placeholder:text-text-muted"
            placeholder="key"
            spellcheck="false"
            autocomplete="off"
        />
        <input
            v-model="newValue"
            @keydown.enter="add"
            ref="valueInputRef"
            class="flex-1 min-w-0 px-3 py-1.5 rounded-lg text-sm border border-border bg-surface text-text outline-none focus:border-primary transition-colors placeholder:text-text-muted"
            placeholder="value"
            spellcheck="false"
            autocomplete="off"
        />
        <button
            @click="add"
            :disabled="!newKey.trim()"
            class="flex items-center justify-center w-8 h-8 rounded-lg bg-primary text-text-inverse hover:bg-primary-hover disabled:opacity-40 disabled:cursor-not-allowed transition-colors shrink-0 active:scale-95"
            aria-label="Add placeholder"
        >
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <!-- Validation error -->
      <Transition
          enter-active-class="transition-all duration-200"
          leave-active-class="transition-opacity duration-150"
          enter-from-class="opacity-0 -translate-y-1"
          leave-to-class="opacity-0"
      >
        <p v-if="error" class="text-xs text-danger flex items-center gap-1.5">
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none" class="shrink-0">
            <circle cx="6" cy="6" r="5.25" stroke="currentColor" stroke-width="1.5"/>
            <path d="M6 3.5V6.5M6 8v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          {{ error }}
        </p>
      </Transition>

    </div>
  </Modal>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import Modal from './Modal.vue'
import { useModal } from './useModal'
import type {Placeholder} from "../dashboard/useRundown.ts";

const MODAL_ID = 'placeholder-editor'

const props = defineProps<{
  placeholders: Placeholder[]
}>()

const emit = defineEmits<{
  set:    [key: string, value: string]
  delete: [key: string]
}>()

const modals       = useModal()
const newKey       = ref('')
const newValue     = ref('')
const error        = ref('')
const keyInputRef  = ref<HTMLInputElement>()
const valueInputRef = ref<HTMLInputElement>()

function sanitizeKey(raw: string) {
  // Strip surrounding % signs if user types them, strip whitespace
  return raw.trim().replace(/^%|%$/g, '').replace(/\s+/g, '_')
}

function focusValue() {
  if (newKey.value.trim()) valueInputRef.value?.focus()
}

function add() {
  error.value = ''
  const key = sanitizeKey(newKey.value)

  if (!key) {
    error.value = 'Key cannot be empty.'
    keyInputRef.value?.focus()
    return
  }
  if (!/^[a-zA-Z0-9_]+$/.test(key)) {
    error.value = 'Key may only contain letters, numbers, and underscores.'
    return
  }
  if (props.placeholders.some(p => p.key === key)) {
    error.value = `%${key}% already exists — edit its value directly above.`
    return
  }

  emit('set', key, newValue.value)
  newKey.value   = ''
  newValue.value = ''
  nextTick(() => keyInputRef.value?.focus())
}

function updateValue(key: string, value: string) {
  emit('set', key, value)
}

function remove(key: string) {
  emit('delete', key)
}

// Expose open() so parent can trigger it without coupling to the modal id
function open() {
  modals.open(MODAL_ID)
}

defineExpose({ open })
</script>