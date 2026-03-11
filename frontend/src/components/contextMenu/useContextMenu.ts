import { reactive, nextTick } from "vue"

interface MenuItem {
    label?: string
    icon?: string
    danger?: boolean
    disabled?: boolean
    type?: "separator"
    action?: () => void
}

const state = reactive({
    visible: false,
    x: 0,
    y: 0,
    items: [] as MenuItem[],
    anchorEl: null as HTMLElement | null
})

async function show(
    mouseX: number,
    mouseY: number,
    items: MenuItem[],
    anchorEl?: HTMLElement
) {
    state.items = items
    state.visible = true
    state.anchorEl = anchorEl ?? null

    await nextTick()

    const menu = document.querySelector(".context-menu") as HTMLElement
    if (!menu) return

    const menuHeight = menu.offsetHeight
    const menuWidth = menu.offsetWidth
    const viewportHeight = window.innerHeight
    const viewportWidth = window.innerWidth

    let x = mouseX
    let y = mouseY

    if (anchorEl) {
        const rect = anchorEl.getBoundingClientRect()

        const spaceBelow = viewportHeight - rect.bottom
        const spaceAbove = rect.top

        if (spaceBelow >= menuHeight) {
            // Place below
            y = rect.bottom
        } else if (spaceAbove >= menuHeight) {
            // Place above
            y = rect.top - menuHeight
        } else {
            // Fallback → above mouse
            y = mouseY - menuHeight
        }

        x = rect.left
    }

    // Clamp horizontally
    if (x + menuWidth > viewportWidth) {
        x = viewportWidth - menuWidth - 8
    }
    if (x < 8) x = 8

    // Clamp vertically
    if (y + menuHeight > viewportHeight) {
        y = viewportHeight - menuHeight - 8
    }
    if (y < 8) y = 8

    state.x = x
    state.y = y
}

function close() {
    state.visible = false
    state.anchorEl = null
}

export function useContextMenu() {
    return {
        state,
        show,
        close
    }
}