// useRundown.ts
import { ref, computed } from "vue"
import { store } from "../shared/store.ts"
import type { WebSocketManager } from "../shared/network/WebSocketManager"

export type RundownRow = Record<string, string>
export type Mode = "production" | "editor"

export function useRundown(socket: WebSocketManager<any>) {

    // ─── State ──────────────────────────────────────────────────────────────────
    const rows        = ref<RundownRow[]>([])
    const headers     = ref<string[]>([])
    const selectedIdx = ref<number | null>(null)
    const selectedSet = ref<Set<number>>(new Set())   // for bulk operations
    const mode        = ref<Mode>("production")
    const isDirty     = ref(false)

    const currentId   = ref<string | null>(null)
    const currentName = ref<string>("")

    const rundownList = ref<{ id: string; name: string }[]>([])

    // ─── Derived ─────────────────────────────────────────────────────────────────
    // Fall back to store keys from FullState when no rundown is loaded
    const effectiveHeaders = computed(() =>
        headers.value.length ? headers.value : Object.keys(store.values)
    )
    const dataHeaders = computed(() => effectiveHeaders.value.filter(h => h.includes("_")))
    const labelHeader = computed(() => effectiveHeaders.value.find(h => !h.includes("_")) ?? null)

    function sceneIds(): string[] {
        return [...new Set(dataHeaders.value.map(h => h.substring(0, h.lastIndexOf("_"))))]
    }

    // ─── Socket events ────────────────────────────────────────────────────────────
    function onServerEvent(event: any) {
        const type = event.type?.split(".").pop()
        switch (type) {
            case "RundownList":
                rundownList.value = event.rundowns
                break
            case "RundownData":
                applyRundownData(event.id, event.name, event.rows)
                break
            case "RundownSaved": {
                const idx = rundownList.value.findIndex(r => r.id === event.id || r.name === event.name)
                if (idx >= 0) rundownList.value[idx] = { id: event.name, name: event.name }
                else rundownList.value.push({ id: event.name, name: event.name })
                currentId.value   = event.name
                currentName.value = event.name
                isDirty.value     = false
                break
            }
            case "RundownDeleted":
                rundownList.value = rundownList.value.filter(r => r.id !== event.id)
                if (currentId.value === event.id) clearCurrent()
                break
        }
    }

    function applyRundownData(id: string, name: string, incomingRows: RundownRow[]) {
        const allKeys = new Set<string>()
        incomingRows.forEach(r => Object.keys(r).forEach(k => allKeys.add(k)))
        headers.value     = [...allKeys]
        rows.value        = incomingRows
        currentId.value   = id
        currentName.value = name
        selectedIdx.value = null
        selectedSet.value = new Set()
        isDirty.value     = false
    }

    function clearCurrent() {
        rows.value        = []
        headers.value     = []
        currentId.value   = null
        currentName.value = ""
        selectedIdx.value = null
        selectedSet.value = new Set()
        isDirty.value     = false
    }

    // ─── Server requests ──────────────────────────────────────────────────────────
    function requestList() {
        socket.send({ type: "ListRundowns" })
    }

    function requestLoad(id: string) {
        socket.send({ type: "LoadRundown", id })
    }

    function saveRundown() {
        if (!currentName.value.trim()) return
        socket.send({
            type: "SaveRundown",
            id:   currentId.value ?? "",
            name: currentName.value.trim(),
            rows: rows.value,
        })
    }

    function deleteRundown(id: string) {
        socket.send({ type: "DeleteRundown", id })
    }

    function renameRundown(id: string, newName: string) {
        if (!newName.trim() || newName === id) return
        const existing = rundownList.value.find(r => r.id === id)
        if (!existing) return
        const rowsToSend = currentId.value === id ? rows.value : []
        socket.send({ type: "SaveRundown", id, name: newName.trim(), rows: rowsToSend })
        if (currentId.value === id) {
            currentId.value   = newName.trim()
            currentName.value = newName.trim()
        }
    }

    function createNewRundown(name: string) {
        rows.value        = []
        // Preserve headers from last rundown so columns are known
        selectedIdx.value = null
        selectedSet.value = new Set()
        currentId.value   = null
        currentName.value = name.trim()
        isDirty.value     = false
    }

    // ─── Row operations ───────────────────────────────────────────────────────────
    function markDirty() { isDirty.value = true }

    function emptyRow(): RundownRow {
        const r: RundownRow = {}
        if (labelHeader.value) r[labelHeader.value] = ""
        dataHeaders.value.forEach(h => r[h] = "")
        return r
    }

    function insertRowAt(idx: number) {
        rows.value.splice(idx, 0, emptyRow())
        selectedIdx.value = idx
        selectedSet.value = new Set()
        markDirty()
    }

    function insertRowAbove() {
        const at = selectedIdx.value ?? 0
        insertRowAt(at)
    }

    function insertRowBelow() {
        const at = selectedIdx.value === null ? rows.value.length : selectedIdx.value + 1
        insertRowAt(at)
    }

    function deleteRow(idx: number) {
        rows.value.splice(idx, 1)
        if (selectedIdx.value === idx) selectedIdx.value = null
        else if (selectedIdx.value !== null && selectedIdx.value > idx) selectedIdx.value--
        selectedSet.value = new Set()
        markDirty()
    }

    function bulkDelete() {
        const toDelete = [...selectedSet.value].sort((a, b) => b - a)
        toDelete.forEach(i => rows.value.splice(i, 1))
        selectedIdx.value = null
        selectedSet.value = new Set()
        markDirty()
    }

    function moveRow(fromIdx: number, toIdx: number) {
        if (fromIdx === toIdx) return
        const [row] = rows.value.splice(fromIdx, 1)
        rows.value.splice(toIdx, 0, row as any)
        selectedIdx.value = toIdx
        markDirty()
    }

    function updateRowField(idx: number, flatKey: string, value: string) {
        if (!rows.value[idx]) return
        rows.value[idx][flatKey] = value
        markDirty()
    }

    function appendFromStore() {
        if (!dataHeaders.value.length) return
        const newRow: RundownRow = {}
        if (labelHeader.value) newRow[labelHeader.value] = ""
        dataHeaders.value.forEach(k => newRow[k] = store.values[k] ?? "")
        rows.value.push(newRow)
        selectedIdx.value = rows.value.length - 1
        selectedSet.value = new Set()
        markDirty()
    }

    function replaceRowFromStore(idx: number) {
        if (!rows.value[idx]) return
        const label = labelHeader.value ? rows.value[idx][labelHeader.value] : undefined
        const newRow: RundownRow = {}
        if (labelHeader.value) newRow[labelHeader.value] = label ?? ""
        dataHeaders.value.forEach(k => newRow[k] = store.values[k] ?? "")
        rows.value[idx] = newRow
        markDirty()
    }

    // ─── Flip two scenes' values within a row ────────────────────────────────────
    function flipScenes(sceneA: string, sceneB: string, rowIdx: number) {
        const row = rows.value[rowIdx]
        if (!row) return

        const keysA = dataHeaders.value.filter(h => h.startsWith(sceneA + "_"))
        const keysB = dataHeaders.value.filter(h => h.startsWith(sceneB + "_"))

        // Swap matching field suffixes between the two scenes
        // e.g. LeftPersonSuper_title ↔ RightPersonSuper_title
        const suffixesA = new Set(keysA.map(k => k.substring(k.lastIndexOf("_"))))
        const suffixesB = new Set(keysB.map(k => k.substring(k.lastIndexOf("_"))))
        const shared = [...suffixesA].filter(s => suffixesB.has(s))

        for (const suffix of shared) {
            const keyA = sceneA + suffix
            const keyB = sceneB + suffix
            const tmp = row[keyA] ?? ""
            row[keyA] = row[keyB] ?? ""
            row[keyB] = tmp
        }

        // Trigger reactivity
        rows.value[rowIdx] = { ...row }
        markDirty()
    }

    // ─── Toggle selection ─────────────────────────────────────────────────────────
    function toggleBulkSelect(idx: number) {
        if (selectedSet.value.has(idx)) selectedSet.value.delete(idx)
        else selectedSet.value.add(idx)
        // Mirror to trigger reactivity
        selectedSet.value = new Set(selectedSet.value)
    }

    function clearBulkSelect() {
        selectedSet.value = new Set()
    }

    // ─── XPression emit helpers (returned so components can call them) ────────────
    function sceneIdsForRow(row: RundownRow): { sceneId: string; hasContent: boolean }[] {
        return sceneIds().map(sceneId => {
            const values = dataHeaders.value
                .filter(h => h.startsWith(sceneId + "_"))
                .map(h => row?.[h] ?? "")
            return { sceneId, hasContent: values.some(v => v !== "") }
        })
    }

    return {
        // state
        rows, headers, selectedIdx, selectedSet,
        mode, isDirty, currentId, currentName, rundownList,
        // derived
        dataHeaders, labelHeader,
        // socket
        onServerEvent, requestList, requestLoad,
        saveRundown, deleteRundown, renameRundown, createNewRundown,
        // row ops
        insertRowAbove, insertRowBelow, insertRowAt, deleteRow, bulkDelete,
        moveRow, updateRowField, appendFromStore, replaceRowFromStore, flipScenes,
        toggleBulkSelect, clearBulkSelect,
        // helpers
        sceneIdsForRow, emptyRow,
    }
}