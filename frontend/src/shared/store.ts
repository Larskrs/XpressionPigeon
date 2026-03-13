import { reactive } from "vue"
import type { Rundown, RundownMeta } from "../dashboard/useRundown"

export const store = reactive({
    // ── Scenes ────────────────────────────────────────────────────────────────
    values: {} as Record<string, string>,

    setValues(data: Record<string, string>) {
        this.values = { ...data }
    },

    updateValue(key: string, value: string) {
        this.values[key] = value
    },

    // ── Rundowns ──────────────────────────────────────────────────────────────
    rundownIndex: [] as RundownMeta[],
    currentRundown: null as Rundown | null,

    setRundownIndex(index: RundownMeta[]) {
        this.rundownIndex = index
    },

    upsertRundownMeta(id: string, name: string, previousId = id) {
        const idx = this.rundownIndex.findIndex(r => r.id === previousId)
        if (idx !== -1) {
            this.rundownIndex[idx] = { id, name }
        } else {
            this.rundownIndex.push({ id, name })
        }
    },

    removeRundown(id: string) {
        this.rundownIndex = this.rundownIndex.filter(r => r.id !== id)
        if (this.currentRundown?.id === id) this.currentRundown = null
    },

    setCurrentRundown(rundown: Rundown) {
        this.currentRundown = rundown
    },

    clearCurrentRundown() {
        this.currentRundown = null
    },

    renameRundown(id: string, name: string) {
        const meta = this.rundownIndex.find(r => r.id === id)
        if (meta) meta.name = name
        if (this.currentRundown?.id === id) this.currentRundown.name = name
    },
})