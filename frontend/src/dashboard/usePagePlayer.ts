import { ref, shallowRef, computed, readonly } from "vue"
import { onMounted, onUnmounted, type ComputedRef } from "vue"
import type { Page, Row } from "./useRundown.ts"

export interface PlayerSceneApi {
    scenes:      ComputedRef<Record<string, Record<string, string>>>
    updateField: (sceneId: string, key: string, value: string) => void
    takeScene:   (sceneId: string) => void
    outScene:    (sceneId: string) => void
}

export type PlayerStatus = "idle" | "playing" | "paused"

const TICK_MS = 50

export function usePagePlayer(api: PlayerSceneApi) {

// inside usePagePlayer, after defining the functions:
    onMounted(() => window.addEventListener('keydown', onKeyDown))
    onUnmounted(() => window.removeEventListener('keydown', onKeyDown))

    // ── State ─────────────────────────────────────────────────────────────────

    const status  = ref<PlayerStatus>("idle")
    const page    = shallowRef<Page | null>(null)
    const elapsed = ref(0)  // ms into the page timeline

    // The row currently taken (null = gap between rows or past the end)
    const activeRow = ref<Row | null>(null)

    let timer:    ReturnType<typeof setInterval> | null = null
    let lastTick: number = 0

    // ── Derived ───────────────────────────────────────────────────────────────

    const rows = computed<Row[]>(() => page.value?.rows ?? [])

    /** End of the last row window — used as total page length for the progress bar */
    const totalDuration = computed<number>(() => {
        let max = 0
        for (const row of rows.value) {
            const end = (row.startTime + row.duration) * 1000
            if (end > max) max = end
        }
        return max
    })

    const progress = computed<number>(() => {
        if (!totalDuration.value) return 0
        return Math.min(elapsed.value / totalDuration.value, 1)
    })

    /**
     * The single row whose window [startTime, startTime + duration) contains
     * the current playhead. Returns null when the playhead is in a gap.
     * Rows with duration === 0 are never auto-matched (they require manual advance).
     */

    const rowAtPlayhead = computed<Row | null>(() => {
        const t = elapsed.value
        for (const row of rows.value) {
            if (row.startTime * 1000 > t) break
            if (row.duration === 0) continue
            if (t < (row.startTime + row.duration) * 1000) return row
        }
        return null
    })

    const isPlaying = computed(() => status.value === "playing")
    const isPaused  = computed(() => status.value === "paused")
    const isIdle    = computed(() => status.value === "idle")

    // ── Scene helpers ─────────────────────────────────────────────────────────

    /**
     * Same logic as onSelectRow — full diff against all live scene fields.
     * Sets each field to the row's value or "" if the row doesn't mention it.
     * Takes scenes that end up with content, outs scenes that go fully blank.
     */
    function applyRow(row: Row) {
        const rowValues = new Map(row.values.map(p => [p.name, p.value]))

        for (const [sceneId, fields] of Object.entries(api.scenes.value)) {
            let hasValue = false

            for (const [field, currentValue] of Object.entries(fields)) {
                const key      = `${sceneId}.${field}`
                const newValue = rowValues.get(key) ?? ""
                if (newValue !== currentValue) {
                    api.updateField(sceneId, field, newValue)
                }
                if (newValue !== "") hasValue = true
            }

            if (hasValue) api.takeScene(sceneId)
            else          api.outScene(sceneId)
        }

        activeRow.value = row
    }

    /** Out all scenes and clear the active row. */
    function outAll() {
        for (const [sceneId] of Object.entries(api.scenes.value)) {
            api.outScene(sceneId)
        }
        activeRow.value = null
    }
    function takeAll() {
        for (const sceneId of getFilledScenes()) {
           api.takeScene(sceneId)
        }
        activeRow.value = null
    }

    function getFilledScenes() {
        let scenes: string[] = [];
        for (const [sceneId, parameters] of Object.entries(api.scenes.value)) {
            let params: any[] = []
            for (const p of Object.values(parameters)) {
                if (!p) continue
                params = [...params, p]
            }
            if (params.length <= 0) continue
            console.log()
            scenes = [...scenes, sceneId]
        }
        return scenes
    }

    /**
     * Clear every field to "" and out every scene.
     * Called once when play starts from idle so output begins from a clean slate.
     */
    function clearAllScenes() {
        for (const [sceneId, fields] of Object.entries(api.scenes.value)) {
            for (const [field, currentValue] of Object.entries(fields)) {
                if (currentValue !== "") api.updateField(sceneId, field, "")
            }
            api.outScene(sceneId)
        }
        activeRow.value = null
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    function startTimer() {
        stopTimer()
        lastTick = performance.now()
        timer = setInterval(tick, TICK_MS)
    }

    function stopTimer() {
        if (timer !== null) {
            clearInterval(timer)
            timer = null
        }
    }

    function tick() {
        const now   = performance.now()
        elapsed.value += now - lastTick
        lastTick = now

        // Past the end of all rows — stop automatically
        if (totalDuration.value > 0 && elapsed.value >= totalDuration.value) {
            outAll()
            stopTimer()
            status.value  = "idle"
            elapsed.value = 0
            return
        }

        const hit = rowAtPlayhead.value

        if (hit && hit !== activeRow.value) {
            // Playhead just entered a new row's window
            applyRow(hit)
        } else if (!hit && activeRow.value !== null) {
            // Playhead moved into a gap — out the previous row
            outAll()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    function loadPage(p: Page) {
        stop()
        page.value    = p
        elapsed.value = 0
    }

    function play() {
        if (!page.value || rows.value.length === 0) return

        if (status.value === "idle") {
            clearAllScenes()
            // If the playhead is already inside a row's window, apply it immediately
            const hit = rowAtPlayhead.value
            if (hit) applyRow(hit)
        }

        status.value = "playing"
        startTimer()
    }

    function pause() {
        if (status.value !== "playing") return
        stopTimer()
        status.value = "paused"
    }

    function togglePlay() {
        if (status.value === "playing") pause()
        else play()
    }

    function stop() {
        stopTimer()
        outAll()
        status.value  = "idle"
        elapsed.value = 0
    }

    /** Jump the playhead to a specific ms position on the page timeline. */
    function seekTo(ms: number) {
        elapsed.value = Math.max(0, ms)
        const hit = rowAtPlayhead.value
        if (hit) applyRow(hit)
        else outAll()
    }

    /** Jump directly to the start of a row by index. */
    function jumpToRow(index: number) {
        const row = rows.value[index]
        if (!row) return
        seekTo(row.startTime * 1000)
    }

    /** Index of the row whose window contains the playhead, or -1. */
    const activeRowIndex = computed(() => {
        const t = elapsed.value
        return rows.value.findIndex(r =>
            r.duration > 0 && t >= r.startTime * 1000 && t < (r.startTime + r.duration) * 1000
        )
    })

    /** Skip to the next row's start time. */
    function skipNext() {
        const idx = activeRowIndex.value
        const next = idx === -1
            ? rows.value.find(r => r.startTime * 1000 > elapsed.value)
            : rows.value[idx + 1]
        if (next) seekTo(next.startTime * 1000)
    }

    /** Skip to the previous row's start time, or restart the current one. */
    function skipPrev() {
        const idx = activeRowIndex.value
        if (idx > 0) {
            seekTo(rows.value[idx - 1]!.startTime * 1000)
        } else if (idx === 0) {
            seekTo(0)
        } else {
            // In a gap — find the last row before the playhead
            for (let i = rows.value.length - 1; i >= 0; i--) {
                if (rows.value[i]!.startTime * 1000 < elapsed.value) {
                    seekTo(rows.value[i]!.startTime * 1000)
                    return
                }
            }
            seekTo(0)
        }
    }

    function isInputFocused(): boolean {
        const el = document.activeElement
        if (!el) return false
        const tag = el.tagName.toLowerCase()
        return tag === 'input' || tag === 'textarea' || tag === 'select'
            || (el as HTMLElement).isContentEditable
    }

    function onKeyDown(e: KeyboardEvent) {
        if (isInputFocused()) return

        if (e.code === "PageDown") {
            outAll()
        } else if (e.code === "PageUp") {
            takeAll()
        } else if (e.code === 'Space') {
            e.preventDefault()
            togglePlay()
        } else if (e.code === "ArrowDown") {
            e.preventDefault()
            clearAllScenes()
        } else if (e.code === 'ArrowRight') {
            e.preventDefault()
            skipNext()
        } else if (e.code === 'ArrowLeft') {
            e.preventDefault()
            skipPrev()
        }
    }

    return {
        // Reactive state
        status:        readonly(status),
        page:          readonly(page),
        elapsed:       readonly(elapsed),
        activeRow:     readonly(activeRow),
        // Derived
        rows,
        rowAtPlayhead,
        totalDuration,
        progress,
        isPlaying,
        isPaused,
        isIdle,
        activeRowIndex,
        // Actions
        loadPage,
        play,
        pause,
        togglePlay,
        stop,
        seekTo,
        jumpToRow,
        skipNext,
        skipPrev,
    }
}