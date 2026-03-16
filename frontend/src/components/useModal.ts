import { reactive, readonly } from "vue"

interface ModalState {
  open: boolean
  data: Record<string, unknown>
}

const registry = reactive<Record<string, ModalState>>({})

function ensureRegistered(id: string): ModalState {
  if (!registry[id]) registry[id] = { open: false, data: {} }
  return registry[id] as ModalState
}

export function useModal() {
  return {
    open(id: string) {
      ensureRegistered(id).open = true
    },
    openWithData(id: string, data: Record<string, unknown>) {
      const entry = ensureRegistered(id)
      entry.data = data
      entry.open = true
    },
    close(id: string) {
      const entry = ensureRegistered(id)
      entry.open = false
      entry.data = {}
    },
    isOpen(id: string): boolean {
      return ensureRegistered(id).open
    },
    getData<T extends Record<string, unknown> = Record<string, unknown>>(id: string): Readonly<T> {
      return readonly(ensureRegistered(id).data) as Readonly<T>
    },
    _setState(id: string, open: boolean) {
      const entry = ensureRegistered(id)
      entry.open = open
      if (!open) entry.data = {}
    },
  }
}