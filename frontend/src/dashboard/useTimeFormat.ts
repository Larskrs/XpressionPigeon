export function formatTime(ms: number): string {
    const s = Math.floor(ms / 1000)
    const m = Math.floor(s / 60)
    const h = Math.floor(m / 60)
    return [h, m % 60, s % 60]
        .map((v, i) => (i === 0 && v === 0 ? null : String(v).padStart(2, "0")))
        .filter(Boolean)
        .join(":")
}

export function duration(startTime: number, endTime: number): string {
    return formatTime(endTime - startTime)
}