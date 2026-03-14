/* -------------------------------------------------- */
/* Types                                              */
/* -------------------------------------------------- */

export type WebSocketEvent =
    | "open"
    | "message"
    | "close"
    | "reconnect"
    | "error"

export interface WebSocketOptions {
    retryDelay?: number
    debug?: boolean
}

export type PathBuilder = () => string

/* -------------------------------------------------- */
/* Defaults                                           */
/* -------------------------------------------------- */

const DEFAULTS: Required<WebSocketOptions> = {
    retryDelay: 5000,
    debug: false
}

/* -------------------------------------------------- */
/* WebSocketManager                                   */
/* -------------------------------------------------- */

export class WebSocketManager<TMessage = unknown> {

    private pathBuilder: PathBuilder
    private options: Required<WebSocketOptions>

    private socket: WebSocket | null = null

    private manualClose = false
    private isConnecting = false
    private reconnectTimer: ReturnType<typeof setTimeout> | null = null

    private listeners: Record<WebSocketEvent, Set<(data?: any) => void>> = {
        open: new Set(),
        message: new Set(),
        close: new Set(),
        reconnect: new Set(),
        error: new Set()
    }

    constructor(pathBuilder: PathBuilder, options: WebSocketOptions = {}) {
        this.pathBuilder = pathBuilder
        this.options = { ...DEFAULTS, ...options }
    }

    /* -------------------------------------------------- */
    /* Public API                                         */
    /* -------------------------------------------------- */

    connect(): void {
        if (this.isConnecting) return

        this.isConnecting = true

        if (this.socket) {
            this.manualClose = true
            this.socket.close()
        }

        const url = this.pathBuilder()

        if (this.options.debug) {
            console.log("[WS] Connecting:", url)
        }

        this.socket = new WebSocket(url)

        this.socket.onopen = () => {
            this.isConnecting = false
            this.clearReconnect()

            if (this.options.debug) {
                console.log("[WS] Connected")
            }

            this.emit("open")
        }

        this.socket.onmessage = (e: MessageEvent) => {
            try {
                const parsed = JSON.parse(e.data) as TMessage
                this.emit("message", parsed)
            } catch (err) {
                if (this.options.debug) {
                    console.warn("[WS] Invalid JSON", err)
                }
            }
        }

        this.socket.onerror = (err: Event) => {
            this.emit("error", err)
        }

        this.socket.onclose = () => {
            this.isConnecting = false

            if (this.manualClose) {
                this.manualClose = false
                return
            }

            if (this.options.debug) {
                console.log("[WS] Closed — scheduling retry")
            }

            this.emit("close")
            this.startReconnect()
        }
    }

    disconnect(): void {
        if (!this.socket) return

        this.manualClose = true
        this.clearReconnect()
        this.socket.close()
    }

    send(payload: unknown): void {
        if (this.socket?.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(payload))
        }
    }

    on(event: WebSocketEvent, callback: (data?: any) => void): void {
        this.listeners[event].add(callback)
    }

    off(event: WebSocketEvent, callback: (data?: any) => void): void {
        this.listeners[event].delete(callback)
    }

    status() {
        return this.socket?.readyState
    }

    /* -------------------------------------------------- */
    /* Private                                            */
    /* -------------------------------------------------- */

    private startReconnect(): void {
        if (this.reconnectTimer) return

        if (this.options.debug) {
            console.log(`[WS] Retrying in ${this.options.retryDelay}ms`)
        }

        this.emit("reconnect", { delay: this.options.retryDelay })

        this.reconnectTimer = setTimeout(() => {
            this.clearReconnect()
            this.connect()
        }, this.options.retryDelay)
    }

    private clearReconnect(): void {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer)
            this.reconnectTimer = null
        }
    }

    private emit(event: WebSocketEvent, data?: any): void {
        this.listeners[event].forEach(cb => cb(data))
    }
}