<!-- SceneInput.vue -->
<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue"
import type { Placeholder } from "../useRundown.ts"

const props = defineProps<{
  label: string
  modelValue: string
  placeholder?: string
  placeholders?: Placeholder[]
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  "commit": [value: string]
}>()

const localValue = ref(props.modelValue)
const isFocused  = ref(false)
const isFloated  = computed(() => isFocused.value || localValue.value !== "")

watch(() => props.modelValue, (val) => {
  if (!isFocused.value) localValue.value = val
})

function commit() {
  emit("update:modelValue", localValue.value)
  emit("commit", localValue.value)
}

// ── Autocomplete ──────────────────────────────────────────────────────────────

const textareaRef = ref<HTMLTextAreaElement>()

// acMode: 'key' = user typed %, 'value' = text matches a placeholder value
const acMode  = ref<'key' | 'value' | null>(null)
const acQuery = ref('')   // partial key typed after %
const acStart = ref(-1)   // index of the opening % in localValue
const acIndex = ref(0)    // highlighted row in dropdown

/** Find an unclosed % trigger ending at cursor position. */
function getKeyTrigger(text: string, cursor: number): { query: string; start: number } | null {
  const before = text.slice(0, cursor)
  const lastPct = before.lastIndexOf('%')
  if (lastPct === -1) return null
  const partial = before.slice(lastPct + 1)
  if (!/^[a-zA-Z0-9_]*$/.test(partial)) return null
  return { query: partial, start: lastPct }
}

type Suggestion = Placeholder & { matchType: 'key' | 'value' }

const suggestions = computed((): Suggestion[] => {
  if (!props.placeholders?.length) return []

  if (acMode.value === 'key') {
    const q = acQuery.value.toLowerCase()
    return props.placeholders
      .filter(p => !q || p.key.toLowerCase().includes(q))
      .map(p => ({ ...p, matchType: 'key' as const }))
  }

  if (acMode.value === 'value') {
    const text = localValue.value.trim().toLowerCase()
    return props.placeholders
      .filter(p => p.value && p.value.toLowerCase().includes(text))
      .map(p => ({ ...p, matchType: 'value' as const }))
  }

  return []
})

const showAc = computed(() => isFocused.value && suggestions.value.length > 0)

watch(suggestions, () => { acIndex.value = 0 })

function onInput() {
  const ta = textareaRef.value
  if (!ta) return
  const cursor = ta.selectionStart ?? localValue.value.length
  const trigger = getKeyTrigger(localValue.value, cursor)

  if (trigger) {
    acMode.value  = 'key'
    acQuery.value = trigger.query
    acStart.value = trigger.start
    acIndex.value = 0
    return
  }

  // Value match: show if typed text (≥2 chars) appears in any placeholder value
  const trimmed = localValue.value.trim()
  const hasValueMatch = trimmed.length >= 2 && props.placeholders?.some(
    p => p.value && p.value.toLowerCase().includes(trimmed.toLowerCase())
  )
  acMode.value  = hasValueMatch ? 'value' : null
  acQuery.value = ''
  acStart.value = -1
  acIndex.value = 0
}

function acceptSuggestion(ph: Suggestion) {
  const ta = textareaRef.value
  if (!ta) return

  if (acMode.value === 'key') {
    const cursor = ta.selectionStart ?? localValue.value.length
    const before = localValue.value.slice(0, acStart.value)
    const after  = localValue.value.slice(cursor)
    localValue.value = `${before}%${ph.key}%${after}`
    const newPos = before.length + ph.key.length + 2
    nextTick(() => ta.setSelectionRange(newPos, newPos))
  } else {
    // Replace entire value with the placeholder token
    localValue.value = `%${ph.key}%`
  }

  acMode.value = null
  nextTick(() => { ta.focus(); commit() })
}

function closeAc() {
  acMode.value = null
}

function onKeydown(e: KeyboardEvent) {
  if (showAc.value) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      acIndex.value = (acIndex.value + 1) % suggestions.value.length
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      acIndex.value = (acIndex.value - 1 + suggestions.value.length) % suggestions.value.length
      return
    }
    if (e.key === 'Tab' || (e.key === 'Enter' && !e.shiftKey)) {
      const s = suggestions.value[acIndex.value]
      if (s) { e.preventDefault(); acceptSuggestion(s); return }
    }
    if (e.key === 'Escape') {
      e.preventDefault()
      closeAc()
      return
    }
  }

  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    commit()
    ;(e.target as HTMLTextAreaElement).blur()
  }
}

// ── Highlight overlay ─────────────────────────────────────────────────────────

interface Segment {
  type: 'text' | 'placeholder'
  text: string
  key?: string
  resolved?: string
}

const segments = computed((): Segment[] => {
  const text = localValue.value
  if (!text) return []

  const regex = /%([a-zA-Z0-9_]+)%/g
  const parts: Segment[] = []
  let last = 0
  let match: RegExpExecArray | null

  while ((match = regex.exec(text)) !== null) {
    if (match.index > last) parts.push({ type: 'text', text: text.slice(last, match.index) })
    const ph = props.placeholders?.find(p => p.key === match![1])
    parts.push({ type: 'placeholder', text: match[0], key: match[1], resolved: ph?.value })
    last = match.index + match[0].length
  }
  if (last < text.length) parts.push({ type: 'text', text: text.slice(last) })
  return parts
})

const hasPlaceholders = computed(() => segments.value.some(s => s.type === 'placeholder'))
const hoveredKey = ref<string | null>(null)
</script>

<template>
  <div class="relative mt-0">
    <!-- Floating label -->
    <label
        class="absolute left-3 transition-all tracking-widest duration-250 ease-in-out pointer-events-none z-10 origin-left"
        :class="isFloated
          ? 'top-0 -translate-y-1/2 text-zinc-500 text-xs bg-zinc-950 px-1.5'
          : 'top-[0.6rem] translate-y-0 text-sm text-zinc-600 bg-transparent px-0'"
    >{{ label }}</label>

    <textarea
        ref="textareaRef"
        v-model="localValue"
        rows="1"
        @focus="isFocused = true"
        @blur="isFocused = false; closeAc(); commit()"
        @keydown="onKeydown"
        @input="onInput"
        class="w-full bg-zinc-950 text-zinc-100 text-[12px] rounded px-3 py-2 resize-none leading-relaxed border border-zinc-700 hover:border-zinc-600 focus:border-zinc-500 focus:outline-none transition-colors"
        style="field-sizing: content; min-height: 2.25rem;"
    />

    <!--
      Highlight overlay — pointer-events:none overall.
      Placeholder spans opt back in for hover tooltips.
    -->
    <div
        v-if="hasPlaceholders"
        class="absolute inset-px px-3 py-2 text-[12px] leading-relaxed pointer-events-none rounded"
        style="white-space: pre-wrap; word-break: break-word;"
        aria-hidden="true"
    >
      <template v-for="(seg, i) in segments" :key="i">
        <span
            v-if="seg.type === 'placeholder'"
            class="relative inline pointer-events-auto cursor-default select-none text-transparent bg-amber-400/25 rounded-[3px] outline outline-1 outline-amber-500/40"
            @mouseenter="hoveredKey = seg.key!"
            @mouseleave="hoveredKey = null"
            @click.prevent="textareaRef?.focus()"
        >{{ seg.text }}<Transition
            enter-active-class="transition-opacity duration-150"
            leave-active-class="transition-opacity duration-100"
            enter-from-class="opacity-0"
            leave-to-class="opacity-0"
        ><span
            v-if="hoveredKey === seg.key"
            class="absolute bottom-full left-1/2 -translate-x-1/2 mb-1.5 z-50 whitespace-nowrap rounded-md bg-zinc-800 border border-zinc-700 px-2 py-1 text-xs text-zinc-200 shadow-lg pointer-events-none"
        ><span class="text-zinc-100">{{ seg.resolved !== undefined && seg.resolved !== '' ? seg.resolved : '(not set)' }}</span></span></Transition></span>

        <span v-else class="text-transparent pointer-events-none select-none">{{ seg.text }}</span>
      </template>
    </div>

    <!-- Autocomplete dropdown -->
    <Transition
        enter-active-class="transition-all duration-150 ease-out"
        leave-active-class="transition-all duration-100 ease-in"
        enter-from-class="opacity-0 scale-y-95 -translate-y-1"
        leave-to-class="opacity-0 scale-y-95 -translate-y-1"
    >
      <ul
          v-if="showAc"
          class="absolute left-0 right-0 top-full mt-1 z-50 rounded-lg border border-zinc-700 bg-zinc-900 shadow-xl overflow-hidden"
          role="listbox"
      >
        <li class="px-3 py-1 text-[10px] font-mono tracking-widest uppercase text-zinc-600 border-b border-zinc-800 select-none">
          {{ acMode === 'value' ? 'Insert as placeholder' : 'Placeholders' }}
        </li>

        <li
            v-for="(s, i) in suggestions"
            :key="s.key"
            role="option"
            :aria-selected="i === acIndex"
            class="flex items-center gap-2 px-3 py-2 cursor-pointer transition-colors select-none text-xs"
            :class="i === acIndex ? 'bg-zinc-700/60' : 'hover:bg-zinc-800/60'"
            @mousedown.prevent="acceptSuggestion(s)"
            @mousemove="acIndex = i"
        >
          <span class="font-mono text-amber-400 shrink-0">%{{ s.key }}%</span>
          <span v-if="s.value" class="text-zinc-400 truncate">{{ s.value }}</span>
          <span v-else class="text-zinc-700 italic">not set</span>
          <span v-if="s.matchType === 'value'" class="ml-auto text-[10px] text-zinc-600 shrink-0 font-mono">value match</span>
        </li>
      </ul>
    </Transition>
  </div>
</template>
