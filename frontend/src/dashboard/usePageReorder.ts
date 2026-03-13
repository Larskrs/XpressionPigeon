import { ref } from "vue"
import type { Page } from "./useRundown.ts"

export function usePageReorder(getPages: () => Page[], onReorder: (pages: Page[]) => void) {
    const draggingId = ref<string | null>(null)
    const dragOverId = ref<string | null>(null)

    function onDragStart(e: DragEvent, pageId: string) {
        draggingId.value = pageId
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = "move"
            e.dataTransfer.setData("text/plain", pageId)
        }
    }

    function onDragOver(e: DragEvent, pageId: string) {
        e.preventDefault()
        if (e.dataTransfer) e.dataTransfer.dropEffect = "move"
        if (pageId !== draggingId.value) dragOverId.value = pageId
    }

    function onDrop(e: DragEvent, targetId: string) {
        e.preventDefault()
        const sourceId = draggingId.value
        if (!sourceId || sourceId === targetId) return
        const pages = [...getPages()]
        const from  = pages.findIndex(p => p.id === sourceId)
        const to    = pages.findIndex(p => p.id === targetId)
        if (from === -1 || to === -1) return
        const [page] = pages.splice(from, 1)
        if (!page) return
        pages.splice(to, 0, page)
        onReorder(pages)
    }

    function onDragEnd() {
        draggingId.value = null
        dragOverId.value = null
    }

    function moveUp(index: number) {
        const pages = [...getPages()]
        if (index === 0) return
        const [page] = pages.splice(index, 1)
        if (!page) return
        pages.splice(index - 1, 0, page)
        onReorder(pages)
    }

    function moveDown(index: number) {
        const pages = [...getPages()]
        if (index === pages.length - 1) return
        const [page] = pages.splice(index, 1)
        if (!page) return
        pages.splice(index + 1, 0, page)
        onReorder(pages)
    }

    return { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd, moveUp, moveDown }
}