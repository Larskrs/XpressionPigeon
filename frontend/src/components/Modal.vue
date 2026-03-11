<template>
  <Teleport to="body">
    <Transition
        enter-active-class="transition-opacity duration-200 ease-out"
        leave-active-class="transition-opacity duration-150 ease-in"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
    >
      <div
          v-if="isOpen"
          class="fixed inset-0 z-[9999] flex items-center justify-center p-6 bg-black/33 dark:bg-black/50"
          :class="{ 'backdrop-blur-sm': backdrop === 'blur' }"
          @click.self="closeOnBackdrop && close()"
          role="dialog"
          :aria-modal="true"
          :aria-labelledby="titleId"
      >
        <Transition
            enter-active-class="transition-all duration-[250ms] ease-[cubic-bezier(.22,.68,0,1.2)]"
            leave-active-class="transition-all duration-200 ease-in"
            enter-from-class="opacity-0 translate-y-4 scale-[.97]"
            leave-to-class="opacity-0 translate-y-2 scale-[.98]"
        >
          <div
              v-if="isOpen"
              ref="modalRef"
              class="relative flex flex-col w-full bg-surface border border-border rounded-2xl shadow-2xl max-h-[calc(100vh-3rem)] overflow-hidden"
              :class="sizeClass"
          >
            <!-- Header -->
            <div
                v-if="$slots.header || title"
                class="flex items-center justify-between gap-4 px-6 pt-5 pb-4 border-b border-border flex-shrink-0"
            >
              <slot name="header">
                <h2
                    :id="titleId"
                    class="m-0 text-xl font-semibold tracking-tight text-text leading-snug"
                >
                  {{ title }}
                </h2>
              </slot>
              <button
                  v-if="showClose"
                  class="flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-lg text-text-muted hover:bg-muted hover:text-text transition-colors"
                  @click="close"
                  aria-label="Lukk"
              >
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                  <path d="M1 1L17 17M17 1L1 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-6 py-5 overflow-y-auto flex-1 text-[0.95rem] leading-relaxed text-text bg-bg">
              <slot :data="modalData" />
            </div>

            <!-- Custom footer slot -->
            <div
                v-if="$slots.footer"
                class="flex items-center justify-end gap-3 px-6 py-4 flex-shrink-0 flex-wrap"
            >
              <slot name="footer" :close="close" />
            </div>

            <!-- Default footer -->
            <div
                v-else-if="confirmLabel || cancelLabel || action"
                class="flex items-center justify-end gap-3 px-6 py-4 bg-bg flex-shrink-0 flex-wrap"
            >
              <!-- Error message -->
              <Transition
                  enter-active-class="transition-all duration-200"
                  leave-active-class="transition-opacity duration-150"
                  enter-from-class="opacity-0 -translate-y-1"
                  leave-to-class="opacity-0"
              >
                <span
                    v-if="actionError"
                    class="flex items-center gap-1.5 w-full text-xs font-medium text-danger bg-danger/10 border border-danger/30 rounded-lg px-3 py-2 mb-1"
                    role="alert"
                >
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true" class="flex-shrink-0">
                    <circle cx="7" cy="7" r="6.25" stroke="currentColor" stroke-width="1.5"/>
                    <path d="M7 4v3.5M7 9.5v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                  {{ actionError }}
                </span>
              </Transition>

              <!-- Cancel -->
              <button
                  v-if="cancelLabel"
                  class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium border border-border text-text-muted bg-transparent hover:bg-muted hover:text-text transition-colors disabled:opacity-50 disabled:cursor-not-allowed active:scale-[.97]"
                  :disabled="actionLoading"
                  @click="cancel"
              >
                {{ cancelLabel }}
              </button>

              <!-- Confirm (no action fn) -->
              <button
                  v-if="confirmLabel && !action"
                  class="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium text-text-inverse transition-colors active:scale-[.97]"
                  :class="confirmBtnClass"
                  @click="confirm"
              >
                {{ confirmLabel }}
              </button>

              <!-- Action button -->
              <button
                  v-if="action"
                  class="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium text-text-inverse transition-colors active:scale-[.97] disabled:opacity-60 disabled:cursor-not-allowed"
                  :class="[confirmBtnClass, { 'opacity-80 cursor-not-allowed': actionLoading }]"
                  :disabled="actionLoading"
                  @click="runAction"
              >
                <svg
                    v-if="actionLoading"
                    class="animate-spin flex-shrink-0"
                    width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true"
                >
                  <circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="2" stroke-dasharray="26" stroke-dashoffset="10" stroke-linecap="round"/>
                </svg>
                {{ actionLoading ? (actionLoadingLabel || actionLabel || confirmLabel) : (actionLabel || confirmLabel) }}
              </button>
            </div>

          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useModal } from './composables/useModal'

const props = defineProps({
  modelValue:       { type: Boolean, default: false },
  id:               { type: String,  default: '' },
  title:            { type: String,  default: '' },
  size:             { type: String,  default: 'md', validator: (v) => ['xs', 'sm', 'md', 'lg', 'xl', 'full'].includes(v as string) },
  variant:          { type: String,  default: 'default', validator: (v) => ['default', 'danger', 'success', 'info'].includes(v as string) },
  backdrop:         { type: String,  default: 'dim', validator: (v) => ['dim', 'blur', 'none'].includes(v as string) },
  closeOnBackdrop:  { type: Boolean, default: true },
  closeOnEsc:       { type: Boolean, default: true },
  showClose:        { type: Boolean, default: true },
  confirmLabel:     { type: String,  default: '' },
  cancelLabel:      { type: String,  default: '' },
  action:           { type: Function, default: null },
  actionLabel:      { type: String,  default: '' },
  actionLoadingLabel: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'close', 'confirm', 'cancel'])

const sizeClass = computed(() => ({
  xs:   'max-w-xs',
  sm:   'max-w-sm',
  md:   'max-w-xl',
  lg:   'max-w-3xl',
  xl:   'max-w-5xl',
  full: 'max-w-full !max-h-screen !rounded-none',
}[props.size]))

const confirmBtnClass = computed(() => ({
  default: 'bg-primary hover:bg-primary-hover',
  danger:  'bg-danger hover:bg-danger-hover',
  success: 'bg-success hover:bg-success',
  info:    'bg-primary hover:bg-primary-hover',
}[props.variant]))

const titleId       = computed(() => `frida-modal-title-${Math.random().toString(36).slice(2, 8)}`)
const actionLoading = ref(false)
const actionError   = ref('')

const modals    = useModal()
const isOpen    = computed(() => props.id ? modals.isOpen(props.id) : props.modelValue)
const modalData = computed(() => props.id ? modals.getData(props.id) : {})

watch(isOpen, (val) => { if (props.modelValue !== val) emit('update:modelValue', val) })
watch(() => props.modelValue, (val) => { if (props.id) modals._setState(props.id, val) })

function close() {
  actionError.value   = ''
  actionLoading.value = false
  if (props.id) modals._setState(props.id, false)
  emit('update:modelValue', false)
  emit('close')
}
function confirm() { emit('confirm'); close() }
function cancel()  { emit('cancel');  close() }

async function runAction() {
  if (!props.action) return
  actionLoading.value = true
  actionError.value   = ''
  try {
    const result = await props.action(modalData.value)
    const ok  = typeof result === 'object' ? result?.ok    : result === true
    const msg = typeof result === 'object' ? result?.error : ''
    if (ok) { close() } else { actionError.value = msg || 'Noe gikk galt. Prøv igjen.' }
  } catch (err) {
    actionError.value = err instanceof Error ? err.message : 'En uventet feil oppstod.'
  } finally {
    actionLoading.value = false
  }
}

function onKeydown(e: KeyboardEvent) {
  if (props.closeOnEsc && e.key === 'Escape' && isOpen.value) close()
}

watch(isOpen, (open) => { document.body.style.overflow = open ? 'hidden' : '' })

onMounted  (() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => { window.removeEventListener('keydown', onKeydown); document.body.style.overflow = '' })
</script>