import { ref } from "vue"

export const DRAG_TYPE_PAGE = "application/x-xp-page"
export const DRAG_TYPE_ROW  = "application/x-xp-row"

export function useReorder<T extends { id: string }>(
    getItems: () => T[],
    onReorder: (items: T[]) => void,
    dragType: string = "application/x-xp-item"
) {
    const draggingId = ref<string | null>(null)
    const dragOverId = ref<string | null>(null)

    function onDragStart(e: DragEvent, id: string) {
        e.stopPropagation()
        draggingId.value = id
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = "move"
            e.dataTransfer.setData(dragType, id)
            e.dataTransfer.setData("text/plain", id)
        }
    }

    function onDragOver(e: DragEvent, id: string) {
        // Only handle events that match our drag type — wrong types bubble upward
        if (!e.dataTransfer?.types.includes(dragType)) return
        e.preventDefault()
        e.stopPropagation()
        e.dataTransfer.dropEffect = "move"
        if (id !== draggingId.value) dragOverId.value = id
    }

    function onDrop(e: DragEvent, targetId: string) {
        // Only handle events that match our drag type
        if (!e.dataTransfer?.types.includes(dragType)) return
        e.preventDefault()
        e.stopPropagation()
        const sourceId = e.dataTransfer.getData(dragType) || draggingId.value
        draggingId.value = null
        dragOverId.value = null
        if (!sourceId || sourceId === targetId) return
        const items = [...getItems()]
        const from  = items.findIndex(i => i.id === sourceId)
        const to    = items.findIndex(i => i.id === targetId)
        if (from === -1 || to === -1) return
        const [item] = items.splice(from, 1)
        if (!item) return
        items.splice(to, 0, item)
        onReorder(items)
    }

    function onDragEnd(e?: DragEvent) {
        e?.stopPropagation()
        draggingId.value = null
        dragOverId.value = null
    }

    function moveUp(index: number) {
        const items = [...getItems()]
        if (index === 0) return
        const [item] = items.splice(index, 1)
        if (!item) return
        items.splice(index - 1, 0, item)
        onReorder(items)
    }

    function moveDown(index: number) {
        const items = [...getItems()]
        if (index === items.length - 1) return
        const [item] = items.splice(index, 1)
        if (!item) return
        items.splice(index + 1, 0, item)
        onReorder(items)
    }

    return { draggingId, dragOverId, onDragStart, onDragOver, onDrop, onDragEnd, moveUp, moveDown }
}