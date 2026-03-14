export function formatTime(ms: number): string {
    const totalSecs = Math.floor(ms / 1000)
    const h = Math.floor(totalSecs / 3600)
    const m = Math.floor((totalSecs % 3600) / 60)
    const s = totalSecs % 60
    if (h > 0) return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
    return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`
}

/**
 * Parse a raw digit string into milliseconds.
 * Digits are read right-to-left as SS, MM, HH:
 *   "10"    → 10 seconds
 *   "100"   → 1 minute 0 seconds
 *   "1000"  → 10 minutes 0 seconds
 *   "10000" → 1 hour 0 minutes 0 seconds
 * Also supports colon-separated input like "1:30" or "01:05:00".
 */
export function parseTimeInput(raw: string): number {
    const trimmed = raw.trim()
    if (!trimmed) return 0

    // If the user typed colons, parse as H:MM:SS / MM:SS / SS
    if (trimmed.includes(":")) {
        const parts = trimmed.split(":").map(Number)
        if (parts.some(isNaN)) return 0
        if (parts.length === 3) return (((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 60 + (parts[2] ?? 0)) * 1000
        if (parts.length === 2) return ((parts[0] ?? 0) * 60 + (parts[1] ?? 0)) * 1000
        return (parts[0] ?? 0) * 1000
    }

    // Pure digits — read right-to-left as HHMMSS
    const digits = trimmed.replace(/\D/g, "")
    if (!digits) return 0
    const padded = digits.padStart(6, "0")
    const hh = parseInt(padded.slice(0, -4), 10)
    const mm = parseInt(padded.slice(-4, -2), 10)
    const ss = parseInt(padded.slice(-2), 10)
    return ((hh * 60 + mm) * 60 + ss) * 1000
}