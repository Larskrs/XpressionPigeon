<script setup lang="ts">
import { ref, computed } from "vue"
import { Icon } from "@iconify/vue"
import type { usePagePlayer } from "../usePagePlayer.ts"
import { formatTime } from "../useTimeFormat.ts"

type Player = ReturnType<typeof usePagePlayer>

const props = defineProps<{ player: Player }>()

const pageName        = computed(() => props.player.page.value?.name ?? "")
const elapsedDisplay  = computed(() => formatTime(Math.floor(props.player.elapsed.value)))
const totalDisplay    = computed(() => formatTime(props.player.totalDuration.value))
const activeRowName   = computed(() => props.player.activeRow.value?.name ?? null)
const progressPct     = computed(() => `${(props.player.progress.value * 100).toFixed(2)}%`)

const hoverPct = ref<number | null>(null)
const hoverTimeDisplay = computed(() => {
  if (hoverPct.value === null) return ""
  const ms = Math.round(hoverPct.value * props.player.totalDuration.value)
  return formatTime(ms)
})

function onTimelineHover(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  hoverPct.value = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
}

function seekFromClick(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const pct  = (e.clientX - rect.left) / rect.width
  props.player.seekTo(Math.round(pct * props.player.totalDuration.value))
}

function rowPct(seconds: number) {
  const total = props.player.totalDuration.value
  return total ? `${((seconds * 1000) / total) * 100}%` : "0%"
}
</script>

<template>
  <Teleport to="body">
    <div
        class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50
               w-[1000px] max-w-[90vw]
               rounded-xl border border-zinc-700/60 bg-zinc-900/97 backdrop-blur-xl shadow-2xl
               select-none shadow-black"
    >
      <div
          v-if="activeRowName"
          class="flex absolute -top-6 left-1/2 -translate-1/2 items-center gap-2 px-3 py-1 rounded bg-zinc-950 border border-blue-800/40"
      >
        <span class="h-1.5 w-1.5 rounded-full bg-blue-500 animate-pulse" />
        <span class="text-[12px] font-mono text-blue-400 uppercase tracking-wider">{{ activeRowName }}</span>
      </div>

      <!-- Timeline bar with row segments -->
      <div class="px-4 pt-4 pb-0.5">
        <div
            class="relative h-2 w-full rounded-full bg-zinc-800 cursor-pointer"
            @click="seekFromClick"
            @mousemove="onTimelineHover"
            @mouseleave="hoverPct = null"
        >

          <!-- Hover tooltip -->
          <Transition name="fade">
            <div
                v-if="hoverPct !== null"
                class="absolute pointer-events-none z-10"
                :style="{ left: `${hoverPct * 100}%`, bottom: '14px', transform: 'translateX(-50%)' }"
            >
              <div class="flex flex-col items-center">
                <div class="bg-white text-zinc-900 text-[11px] font-mono font-medium px-1.5 py-0.5 rounded whitespace-nowrap">
                  {{ hoverTimeDisplay }}
                </div>
                <svg width="8" height="5" viewBox="0 0 8 5" fill="none" class="-mt-px">
                  <path d="M0 0 L4 5 L8 0Z" fill="white" />
                </svg>
              </div>
            </div>
          </Transition>

          <!-- Row segments -->
          <div
              v-for="row in player.rows.value"
              :key="row.id"
              class="absolute inset-y-0 z-1 rounded-full transition-colors cursor-pointer"
              :style="{ left: rowPct(row.startTime), width: rowPct(row.duration) }"
              :class="player.activeRow.value?.id === row.id
                  ? 'bg-blue-600'
                  : player.elapsed.value >= (row.startTime + row.duration) * 1000 && row.duration > 0
                      ? 'bg-zinc-700'
                      : 'bg-zinc-700/50 hover:bg-zinc-600/60'"
              :title="`${row.name} — ${formatTime(row.startTime)} (${formatTime(row.duration)})`"
              @click.stop="player.seekTo(row.startTime * 1000)"
          />

          <!-- Elapsed fill -->
          <div
              class="absolute z-2 inset-y-0 left-0 rounded-full pointer-events-none transition-[width] duration-75"
              v-if="!(player.isIdle.value && player.elapsed.value == 0)"
              :style="{ width: progressPct }"
          >
            <div class="absolute inset-0 rounded-full bg-blue-900/50" />
          </div>

          <!-- Playhead dot -->
          <div
              v-if="!(player.isIdle.value && player.elapsed.value == 0)"
              class="absolute z-2 top-1/2 -translate-y-1/2 -translate-x-1/2 h-3 w-3 rounded-full bg-blue-200 border-2 border-blue-100 shadow pointer-events-none transition-[left] duration-75"
              :style="{ left: progressPct }"
          />
        </div>
      </div>

      <!-- Row labels under timeline -->
      <div v-if="player.rows.value.length > 0" class="relative h-5.5 mx-4 mb-0.5 overflow-x-hidden overflow-y-hidden scrollbar-none">
        <div class="relative h-full" style="min-width: 100%">
          <button
              v-for="row in player.rows.value"
              :key="'l-' + row.id"
              class="absolute whitespace-nowrap text-[11px] font-mono leading-none transition-colors px-0.5 top-0.5"
              :class="player.activeRow.value?.id === row.id ? 'text-blue-400' : 'text-zinc-500 hover:text-zinc-300'"
              :style="{ left: rowPct(row.startTime) }"
              :title="row.name"
              @click="player.seekTo(row.startTime * 1000)"
          >{{ row.name }}</button>
        </div>
      </div>

      <!-- Transport controls — compact row -->
      <div class="flex items-center justify-center gap-0.5 px-4 pb-2.5 pt-0.5">
        <div class="flex items-center gap-2 min-w-0">
          <span class="text-[12px] font-mono text-zinc-500 uppercase tracking-widest truncate">{{ pageName }}</span>
        </div>

        <div class="absolute flex gap-2 bottom-2 left-1/2 -translate-x-1/2">
            <button
                class="p-1.5 rounded-md text-zinc-500 hover:text-zinc-200 hover:bg-zinc-800 transition-colors disabled:opacity-20 disabled:pointer-events-none"
                :disabled="player.isIdle.value"
                title="Previous"
                @click="player.skipPrev()"
            >
              <Icon icon="tabler:player-skip-back-filled" class="size-6" />
            </button>

            <button
                class="p-1.5 rounded-md transition-colors"
                :class="player.isPlaying.value
                    ? 'bg-zinc-700 text-white hover:bg-zinc-600'
                    : 'bg-blue-500 text-white hover:bg-blue-500'"
                :disabled="!player.page.value"
                :title="player.isPlaying.value ? 'Pause' : 'Play'"
                @click="player.togglePlay()"
            >
              <Icon
                  :icon="player.isPlaying.value ? 'tabler:player-pause-filled' : 'tabler:player-play-filled'"
                  class="size-6"
              />
            </button>

            <button
                class="p-1.5 rounded-md text-zinc-500 hover:text-red-400 hover:bg-zinc-800 transition-colors disabled:opacity-20 disabled:pointer-events-none"
                :disabled="player.isIdle.value"
                title="Stop"
                @click="player.stop()"
            >
              <Icon icon="tabler:player-stop-filled" class="size-6" />
            </button>

            <button
                class="p-1.5 rounded-md text-zinc-500 hover:text-zinc-200 hover:bg-zinc-800 transition-colors disabled:opacity-20 disabled:pointer-events-none"
                :disabled="player.isIdle.value"
                title="Next"
                @click="player.skipNext()"
            >
              <Icon icon="tabler:player-skip-forward-filled" class="size-6" />
            </button>
        </div>

        <div class="ml-auto font-mono tabular-nums text-[12px] text-zinc-500 shrink-0">
          {{ elapsedDisplay }} / {{ totalDisplay }}
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.1s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>