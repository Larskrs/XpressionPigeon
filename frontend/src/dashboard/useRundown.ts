import {reactive, toRaw} from "vue"
import type { WebSocketManager } from "../shared/network/WebSocketManager"
import type { ServerEvent } from "./socket"

// ─── Types ────────────────────────────────────────────────────────────────────

export interface Parameter {
    name: string
    value: string
}

export interface Row {
    id: string
    order: number
    name: string
    startTime: number
    duration: number
    notes: string
    values: Parameter[]
}

export interface Page {
    id: string
    order: number
    name: string
    color?: string
    rows: Row[]
}

export interface Rundown {
    id: string
    name: string
    pages: Page[]
}

export interface RundownMeta {
    id: string
    name: string
}

export interface RowReference {
    pageId: string,
    rowId: string
}

interface RundownState {
    index:   RundownMeta[]
    current: Rundown | null
    editing: RowReference | null
    loading: boolean
    error:   string | null
}

// ─── Composable ───────────────────────────────────────────────────────────────

export function useRundown(socket: WebSocketManager<ServerEvent>) {
    const state = reactive<RundownState>({
        index:   [],
        current: null,
        editing: null,
        loading: false,
        error:   null,
    })

    // ── Server → client ───────────────────────────────────────────────────────

    socket.on("message", (event) => {
        const type = event.type?.split(".").pop()

        switch (type) {
            case "RundownList":
                state.index   = (event as any).rundowns
                state.loading = false
                break

            case "RundownData": {
                const e = event as any
                state.current = { id: e.id, name: e.name, pages: e.pages }
                state.loading = false
                break
            }

            case "RundownSaved": {
                const e = event as any
                const idx = state.index.findIndex(r => r.id === e.previousId)
                if (idx !== -1) {
                    state.index[idx] = { id: e.id, name: e.name }
                } else {
                    if (!state.index.some(r => r.id === e.id)) {
                        state.index.push({ id: e.id, name: e.name })
                    }
                }
                if (state.current && state.current.id === e.previousId) {
                    state.current.id   = e.id
                    state.current.name = e.name
                }
                state.loading = false
                break
            }

            case "RundownRenamed": {
                const e = event as any
                const idx = state.index.findIndex(r => r.id === e.id)
                if (idx !== -1) state.index[idx]!.name = e.name
                if (state.current?.id === e.id) state.current!.name = e.name
                break
            }

            case "RundownDeleted": {
                const e = event as any
                state.index = state.index.filter(r => r.id !== e.id)
                if (state.current?.id === e.id) state.current = null
                break
            }

            case "RundownNotFound": {
                const e = event as any
                state.error   = `Rundown not found: ${e.id}`
                state.loading = false
                break
            }
        }
    })

    // ── Client → server ───────────────────────────────────────────────────────

    function listRundowns() {
        state.loading = true
        socket.send({ type: "ListRundowns" })
    }

    function loadRundown(id: string) {
        state.loading = true
        state.error   = null
        socket.send({ type: "LoadRundown", id })
    }

    function saveRundown(r: Rundown = state.current!) {
        const rundown = toRaw(r)
        if (!rundown) return
        state.loading = true
        state.error   = null
        console.log(rundown)
        socket.send({ type: "SaveRundown", id: rundown.id, name: rundown.name, pages: rundown.pages })
    }

    function createRundown(name: string, pages: Page[] = []) {
        state.current = { id: "", name, pages }
        state.loading = true
        state.error   = null
        socket.send({ type: "SaveRundown", id: "", name, pages })
    }

    function renameRundown(id: string, name: string) {
        socket.send({ type: "RenameRundown", id, name })
    }

    function deleteRundown(id: string) {
        socket.send({ type: "DeleteRundown", id })
    }

    // ── Local mutation helpers ────────────────────────────────────────────────

    function addPage(page: Page) {
        if (!state.current) return

        const maxOrder =
            Math.max(-1, ...state.current.pages.map(p => p.order))

        page.order = maxOrder + 1

        state.current.pages.push(page)
        state.current.pages = sortPages(state.current.pages)
    }

    function removePage(pageId: string) {
        if (!state.current) return

        state.current.pages =
            state.current.pages.filter(p => p.id !== pageId)
    }

    function removeRow(pageId: string, rowId: string) {
        const page = state.current?.pages.find(p => p.id === pageId)
        if (!page) return

        page.rows = page.rows.filter((row) => row.id !== rowId)
    }

    function addRow(pageId: string, row: Row) {
        const page = state.current?.pages.find(p => p.id === pageId)
        if (!page) return

        page.rows.push(row)
        page.rows = sortRows(page.rows)
    }

    function updateRow(pageId: string, updated: Row) {
        const page = state.current?.pages.find(p => p.id === pageId)
        if (!page) return

        const i = page.rows.findIndex(r => r.id === updated.id)
        if (i === -1) return

        page.rows[i] = updated
    }

    function sortPages(pages: Page[]) {
        return [...pages].sort((a, b) => a.order - b.order)
    }

    function sortRows(rows: Row[]) {
        return [...rows].sort((a, b) => a.order - b.order)
    }

    function selectEditing(pageId: string, rowId: string) {
        state.editing = { pageId, rowId }
    }

    return {
        state,
        listRundowns,
        loadRundown,
        saveRundown,
        createRundown,
        renameRundown,
        deleteRundown,
        addPage,
        removePage,
        addRow,
        updateRow,
        removeRow,
        selectEditing
    }
}