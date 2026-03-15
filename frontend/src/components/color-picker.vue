<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  "update:modelValue": [color: string]
}>()

const isOpen = ref(false)
const pickerEl = ref<HTMLElement | null>(null)
const canvasEl = ref<HTMLCanvasElement | null>(null)
const sliderEl = ref<HTMLCanvasElement | null>(null)

// ── Presets ───────────────────────────────────────────────────────────────────
const presets = [
  "#e74c3c", "#e67e22", "#f1c40f", "#2ecc71",
  "#1abc9c", "#3498db", "#9b59b6", "#e91e8c",
  "#ff6b6b", "#ffa07a", "#98d8c8", "#74b9ff",
  "#a29bfe", "#fd79a8", "#55efc4", "#ffffff",
]

// ── HSV state ─────────────────────────────────────────────────────────────────
const hue        = ref(0)
const saturation = ref(1)
const value      = ref(1)

// Convert current modelValue to HSV on open
function hexToHsv(hex: string): [number, number, number] {
  const r = parseInt(hex.slice(1, 3), 16) / 255
  const g = parseInt(hex.slice(3, 5), 16) / 255
  const b = parseInt(hex.slice(5, 7), 16) / 255
  const max = Math.max(r, g, b), min = Math.min(r, g, b)
  const d = max - min
  let h = 0
  if (d !== 0) {
    if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) / 6
    else if (max === g) h = ((b - r) / d + 2) / 6
    else h = ((r - g) / d + 4) / 6
  }
  return [h * 360, max === 0 ? 0 : d / max, max]
}

function hsvToHex(h: number, s: number, v: number): string {
  const f = (n: number) => {
    const k = (n + h / 60) % 6
    return v - v * s * Math.max(0, Math.min(k, 4 - k, 1))
  }
  const toHex = (x: number) => Math.round(x * 255).toString(16).padStart(2, "0")
  return `#${toHex(f(5))}${toHex(f(3))}${toHex(f(1))}`
}

const currentHex = computed(() => hsvToHex(hue.value, saturation.value, value.value))

// ── Canvas rendering ──────────────────────────────────────────────────────────
function drawSaturationCanvas() {
  const canvas = canvasEl.value
  if (!canvas) return
  const ctx = canvas.getContext("2d")!
  const w = canvas.width, h = canvas.height

  // White → hue gradient (horizontal)
  const hueColor = hsvToHex(hue.value, 1, 1)
  const gradH = ctx.createLinearGradient(0, 0, w, 0)
  gradH.addColorStop(0, "#ffffff")
  gradH.addColorStop(1, hueColor)
  ctx.fillStyle = gradH
  ctx.fillRect(0, 0, w, h)

  // Transparent → black gradient (vertical)
  const gradV = ctx.createLinearGradient(0, 0, 0, h)
  gradV.addColorStop(0, "rgba(0,0,0,0)")
  gradV.addColorStop(1, "rgba(0,0,0,1)")
  ctx.fillStyle = gradV
  ctx.fillRect(0, 0, w, h)
}

function drawHueSlider() {
  const canvas = sliderEl.value
  if (!canvas) return
  const ctx = canvas.getContext("2d")!
  const grad = ctx.createLinearGradient(0, 0, canvas.width, 0)
  const stops = [0, 60, 120, 180, 240, 300, 360]
  stops.forEach(h => grad.addColorStop(h / 360, `hsl(${h},100%,50%)`))
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, canvas.width, canvas.height)
}

// ── Interaction ───────────────────────────────────────────────────────────────
const isDraggingCanvas = ref(false)
const isDraggingSlider = ref(false)

function pickFromCanvas(e: MouseEvent | TouchEvent) {
  const canvas = canvasEl.value!
  const rect   = canvas.getBoundingClientRect()
  const clientX = "touches" in e ? e.touches[0]!.clientX : e.clientX
  const clientY = "touches" in e ? e.touches[0]!.clientY : e.clientY
  const x = Math.max(0, Math.min(1, (clientX - rect.left)  / rect.width))
  const y = Math.max(0, Math.min(1, (clientY - rect.top)   / rect.height))
  saturation.value = x
  value.value      = 1 - y
  emit("update:modelValue", currentHex.value)
}

function pickFromSlider(e: MouseEvent | TouchEvent) {
  const canvas  = sliderEl.value!
  const rect    = canvas.getBoundingClientRect()
  const clientX = "touches" in e ? e.touches[0]!.clientX : e.clientX
  const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width))
  hue.value = x * 360
  drawSaturationCanvas()
  emit("update:modelValue", currentHex.value)
}

function onCanvasDown(e: MouseEvent) { isDraggingCanvas.value = true; pickFromCanvas(e) }
function onSliderDown(e: MouseEvent) { isDraggingSlider.value = true; pickFromSlider(e) }

function onMouseMove(e: MouseEvent) {
  if (isDraggingCanvas.value) pickFromCanvas(e)
  if (isDraggingSlider.value) pickFromSlider(e)
}
function onMouseUp() {
  isDraggingCanvas.value = false
  isDraggingSlider.value = false
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────
function open() {
  const [h, s, v] = hexToHsv(props.modelValue || "#e74c3c")
  hue.value        = h
  saturation.value = s
  value.value      = v
  isOpen.value     = true
  setTimeout(() => {
    drawSaturationCanvas()
    drawHueSlider()
  }, 0)
}

function onClickOutside(e: MouseEvent) {
  if (pickerEl.value && !pickerEl.value.contains(e.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener("mousemove", onMouseMove)
  window.addEventListener("mouseup",   onMouseUp)
  document.addEventListener("mousedown", onClickOutside)
})
onUnmounted(() => {
  window.removeEventListener("mousemove", onMouseMove)
  window.removeEventListener("mouseup",   onMouseUp)
  document.removeEventListener("mousedown", onClickOutside)
})

// Cursor position in the saturation square
const cursorX = computed(() => `${saturation.value * 100}%`)
const cursorY = computed(() => `${(1 - value.value) * 100}%`)
const sliderX = computed(() => `${(hue.value / 360) * 100}%`)
</script>

<template>
  <div ref="pickerEl" class="relative">
    <!-- Trigger swatch -->
    <button
        class="h-full w-full rounded-sm transition-all duration-150 ring-0 hover:ring-2 hover:ring-white/20 hover:ring-offset-1 hover:ring-offset-zinc-900 focus:outline-none"
        :style="{ background: modelValue || '#222' }"
        @click.stop="isOpen ? isOpen = false : open()"
    />

    <!-- Dropdown panel -->
    <Transition
        enter-active-class="transition-all duration-200 ease-out"
        enter-from-class="opacity-0 scale-95 -translate-y-1"
        enter-to-class="opacity-100 scale-100 translate-y-0"
        leave-active-class="transition-all duration-150 ease-in"
        leave-from-class="opacity-100 scale-100 translate-y-0"
        leave-to-class="opacity-0 scale-95 -translate-y-1"
    >
      <div
          v-if="isOpen"
          class="absolute left-full top-0 ml-2 z-50 w-52 rounded-xl border border-zinc-700/60 bg-zinc-900 shadow-2xl shadow-black/60 p-3 flex flex-col gap-3"
          @click.stop
      >
        <!-- Saturation / value square -->
        <div class="relative rounded-lg overflow-hidden cursor-crosshair select-none" style="height: 140px;">
          <canvas
              ref="canvasEl"
              width="192"
              height="140"
              class="w-full h-full block"
              @mousedown="onCanvasDown"
          />
          <!-- Cursor -->
          <div
              class="pointer-events-none absolute w-3.5 h-3.5 rounded-full border-2 border-white shadow-md -translate-x-1/2 -translate-y-1/2"
              :style="{
                left: cursorX,
                top:  cursorY,
                background: currentHex,
                boxShadow: '0 0 0 1px rgba(0,0,0,0.4), 0 2px 6px rgba(0,0,0,0.5)'
              }"
          />
        </div>

        <!-- Hue slider -->
        <div class="relative rounded-full overflow-hidden cursor-pointer select-none" style="height: 10px;">
          <canvas
              ref="sliderEl"
              width="192"
              height="10"
              class="w-full h-full block"
              @mousedown="onSliderDown"
          />
          <!-- Thumb -->
          <div
              class="pointer-events-none absolute top-1/2 w-3.5 h-3.5 rounded-full border-2 border-white -translate-x-1/2 -translate-y-1/2"
              :style="{
                left: sliderX,
                background: `hsl(${hue}, 100%, 50%)`,
                boxShadow: '0 0 0 1px rgba(0,0,0,0.4), 0 2px 4px rgba(0,0,0,0.5)'
              }"
          />
        </div>

        <!-- Current color + hex display -->
        <div class="flex items-center gap-2">
          <div
              class="w-7 h-7 rounded-md shrink-0 border border-zinc-700"
              :style="{ background: currentHex }"
          />
          <span class="flex-1 font-mono text-xs text-zinc-400 uppercase tracking-widest">
            {{ currentHex }}
          </span>
        </div>

        <!-- Divider -->
        <div class="h-px bg-zinc-800" />

        <!-- Presets -->
        <div class="grid grid-cols-8 gap-1.5">
          <button
              v-for="preset in presets"
              :key="preset"
              class="w-full aspect-square rounded-md transition-all duration-100 hover:scale-110 hover:ring-2 hover:ring-white/30 hover:ring-offset-1 hover:ring-offset-zinc-900 focus:outline-none"
              :class="{ 'ring-2 ring-white/60 ring-offset-1 ring-offset-zinc-900 scale-110': modelValue === preset }"
              :style="{ background: preset }"
              :title="preset"
              @click="emit('update:modelValue', preset)"
          />
        </div>
      </div>
    </Transition>
  </div>
</template>