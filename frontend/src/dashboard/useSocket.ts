// useDashboardSocket.ts - handle all server events
import type { WebSocketManager } from "../shared/network/WebSocketManager"
import type { ServerEvent } from "./socket.ts"
import { store } from "../shared/store.ts"

export function useSocket(socket: WebSocketManager<ServerEvent>) {
    socket.on("message", (event) => {
        const type = event.type?.split(".").pop()

        switch (type) {
            case "FullState":
                console.log(event)
                store.setValues(event.data)
                break
            case "ContentUpdated":
                store.updateValue(`${event.sceneId}_${event.key}`, event.value)
                break
        }
    })
}