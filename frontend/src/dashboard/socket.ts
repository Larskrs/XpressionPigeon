import { WebSocketManager } from "../shared/network/WebSocketManager"

export function createSocket() {
    const proto = location.protocol === "https:" ? "wss" : "ws"
    return new WebSocketManager<ServerEvent>(
        () => `${proto}://${location.host}/ws`,
        { debug: true }
    )
}

export type ServerEvent =
    | { type: "FullState";      data: Record<string, string> }
    | { type: "ContentUpdated"; sceneId: string; key: string; value: string }
    | { type: "SceneTaken";     sceneId: string }
    | { type: "SceneOut";       sceneId: string }
    | { type: "RossTalkStatusChange";       status: string }