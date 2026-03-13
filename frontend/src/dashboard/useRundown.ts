import { reactive, computed } from "vue"
import type { WebSocketManager } from "../shared/network/WebSocketManager"
import type { ServerEvent } from "./socket"

// ─── Types ────────────────────────────────────────────────────────────────────

export interface Parameter {
    name: string
    value: string
}

export interface Row {
    name: string
    startTime: number
    endTime: number
    notes: string
    values: Parameter[]
}

export interface Page {
    id: string
    name: string
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

// ─── Augment ServerEvent ──────────────────────────────────────────────────────
// Add these to the ServerEvent union in socket.ts:
//
//   | { type: "RundownList";    rundowns: RundownMeta[] }
//   | { type: "RundownData";    id: string; name: string; rows: Page[] }
//   | { type: "RundownSaved";   id: string; name: string; previousId: string }
//   | { type: "RundownRenamed"; id: string; name: string }
//   | { type: "RundownDeleted"; id: string }
//   | { type: "RundownNotFound"; id: string }
//   | { type: "RundownsFlushed" }

// ─── State ────────────────────────────────────────────────────────────────────

interface RundownState {
    /** Lightweight index — all known rundowns */
    index: RundownMeta[]
    /** The currently loaded rundown, if any */
    current: Rundown | null
    /** True while a load/save request is in flight */
    loading: boolean
    /** Last error message, cleared on next successful operation */
    error: string | null
}

// ─── Composable ───────────────────────────────────────────────────────────────

export function useRundown(socket: WebSocketManager<ServerEvent>) {
    const state = reactive<RundownState>({
        index:   [],
        current: null,
        loading: false,
        error:   null,
    })

    // ── Derived ───────────────────────────────────────────────────────────────

    /** True when a rundown is open and has at least one page */
    const hasPages = computed(() => (state.current?.pages.length ?? 0) > 0)

    /** Flat list of all rows across all pages in the current rundown */
    const allRows = computed(() =>
        state.current?.pages.flatMap(page => page.rows) ?? []
    )

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
                state.current = { id: e.id, name: e.name, pages: e.rows }
                state.loading = false
                break
            }

            case "RundownSaved": {
                const e = event as any
                console.log(e)
                console.log()
                // Update index entry — handle new rundowns (previousId differs)
                const idx = state.index.findIndex(r => r.id === e.previousId)
                if (idx !== -1) {
                    state.index[idx] = { id: e.id, name: e.name }
                } else {
                    state.index.push({ id: e.id, name: e.name })
                }
                // Patch current if it was the one being saved
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

            case "RundownsFlushed":
                state.loading = false
                break
        }
    })

    // ── Client → server ───────────────────────────────────────────────────────

    /** Fetch the lightweight index of all rundowns. */
    function listRundowns() {
        state.loading = true
        socket.send({ type: "ListRundowns" })
    }

    /** Load the full data for a single rundown by id. */
    function loadRundown(id: string) {
        state.loading = true
        state.error   = null
        socket.send({ type: "LoadRundown", id })
    }

    /**
     * Save the currently loaded rundown.
     * Pass an explicit rundown to save something other than state.current.
     */
    function saveRundown(rundown: Rundown = state.current!) {
        if (!rundown) return
        state.loading = true
        state.error   = null
        socket.send({
            type: "SaveRundown",
            id:   rundown.id,
            name: rundown.name,
            pages: rundown.pages,
        })
    }

    /**
     * Create a brand-new rundown (blank id → server assigns a UUID).
     * The returned shell is stored as current; it will be patched with
     * the real id once the server replies with RundownSaved.
     */
    function createRundown(name: string, pages: Page[] = []) {
        state.current = { id: "", name, pages: pages }
        state.loading = true
        state.error   = null
        socket.send({ type: "SaveRundown", id: "", name, pages: pages })
    }

    /** Rename without touching page data. More efficient than a full save. */
    function renameRundown(id: string, name: string) {
        socket.send({ type: "RenameRundown", id, name })
    }

    function deleteRundown(id: string) {
        socket.send({ type: "DeleteRundown", id })
    }

    /** Force an immediate server-side disk flush of all dirty rundowns. */
    function flushRundowns() {
        state.loading = true
        socket.send({ type: "FlushRundowns" })
    }

    // ── Local mutation helpers ────────────────────────────────────────────────
    // These patch state.current in place without a round-trip.
    // Call saveRundown() afterward to persist.

    function addPage(page: Page) {
        if (!state.current) {
            console.warn("addPage: no current rundown loaded")
            return
        }
        state.current.pages.push(page)
        console.log("addPage: pushed", page.name, "— current has", state.current.pages.length, "pages")
    }

    function removePage(pageId: string) {
        if (!state.current) return
        state.current.pages = state.current.pages.filter(p => p.id !== pageId)
    }

    function addRow(pageId: string, row: Row) {
        const page = state.current?.pages.find(p => p.id === pageId)
        page?.rows.push(row)
    }

    function removeRow(pageId: string, rowName: string) {
        const page = state.current?.pages.find(p => p.id === pageId)
        if (!page) return
        page.rows = page?.rows.filter((r: Row) => r.name !== rowName)
    }

    function updateParameter(pageId: string, rowName: string, paramName: string, value: string) {
        const page = state.current?.pages.find(p => p.id === pageId)
        const row  = page?.rows.find(r => r.name === rowName)
        const param = row?.values.find(v => v.name === paramName)
        if (param) param.value = value
    }

    return {
        // State
        state,
        hasPages,
        allRows,
        // Server ops
        listRundowns,
        loadRundown,
        saveRundown,
        createRundown,
        renameRundown,
        deleteRundown,
        flushRundowns,
        // Local mutation
        addPage,
        removePage,
        addRow,
        removeRow,
        updateParameter,
    }
}