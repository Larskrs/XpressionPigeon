import { computed, reactive } from "vue"
import { store } from "../shared/store.ts"
import type {WebSocketManager} from "../shared/network/WebSocketManager.ts";
import type {ServerEvent} from "./socket.ts";

export interface SceneGroup {
    label: string
    sceneIds: string[]
    flip_enabled?: boolean
}

export const groups: SceneGroup[] = [
    {
        label: "Person Supers",
        sceneIds: ["LeftPersonSuper", "RightPersonSuper"],
        flip_enabled: true,
    },
]

export function useScenes(socket: WebSocketManager<ServerEvent>) {
    const sceneStates = reactive<Record<string, boolean>>({})

    const scenes = computed(() => {
        const grouped: Record<string, Record<string, string>> = {}
        for (const [key, value] of Object.entries(store.values)) {
            const underscore = key.lastIndexOf("_")
            const sceneId = key.substring(0, underscore)
            const field = key.substring(underscore + 1)
            if (!grouped[sceneId]) grouped[sceneId] = {}
            grouped[sceneId][field] = value
        }
        return grouped
    })

    const ungroupedSceneIds = computed(() => {
        const allGrouped = new Set(groups.flatMap(g => g.sceneIds))
        return Object.keys(scenes.value).filter(id => !allGrouped.has(id))
    })

    const takeScene = (id: string) => {
        sceneStates[id] = true
        socket.send({ type: "TakeScene", sceneId: id })
    }

    const outScene = (id: string) => {
        sceneStates[id] = false
        socket.send({ type: "OutScene", sceneId: id })
    }

    const updateField = (sceneId: string, key: string, value: string) => {
        store.updateValue(`${sceneId}_${key}`, value)
        socket.send({ type: "UpdateContent", sceneId, key, value })
    }

    const flipScenes = (idA: string, idB: string) => {
        const fieldsA = { ...(scenes.value[idA] ?? {}) }
        const fieldsB = { ...(scenes.value[idB] ?? {}) }
        const allFields = new Set([...Object.keys(fieldsA), ...Object.keys(fieldsB)])
        for (const field of allFields) {
            updateField(idA, field, fieldsB[field] ?? "")
            updateField(idB, field, fieldsA[field] ?? "")
        }
    }

    const handleFlip = (group: SceneGroup) => {
        const [a, b] = group.sceneIds
        if (a && b) flipScenes(a, b)
    }

    return { scenes, sceneStates, ungroupedSceneIds, takeScene, outScene, updateField, handleFlip }
}