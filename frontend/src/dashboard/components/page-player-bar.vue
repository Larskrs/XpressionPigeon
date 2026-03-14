<script setup lang="ts">
import { computed } from "vue"
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

function seekFromClick(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const pct  = (e.clientX - rect.left) / rect.width
  props.player.seekTo(Math.round(pct * props.player.totalDuration.value))
}

function rowPct(ms: number) {
  const total = props.player.totalDuration.value
  return total ? `${(ms / total) * 100}%` : "0%"
}
</script>

<template>
  <Teleport to="body">
    <div
        class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50
               w-[540px] max-w-[90vw]
               rounded-xl border border-zinc-700/60 bg-zinc-900/97 backdrop-blur-xl shadow-2xl
               select-none overflow-hidden"
    >

      <!-- Header row: page name, live badge, time -->
      <div class="flex items-center justify-between px-4 pt-2.5 pb-1">
        <div class="flex items-center gap-2 min-w-0">
          <span class="text-[10px] font-mono text-zinc-500 uppercase tracking-widest truncate">{{ pageName }}</span>
          <div
              v-if="activeRowName"
              class="flex items-center gap-1 px-1.5 py-px rounded bg-red-950/50 border border-red-800/40"
          >
            <span class="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
            <span class="text-[9px] font-mono text-red-400 uppercase tracking-wider">{{ activeRowName }}</span>
          </div>
        </div>
        <div class="font-mono tabular-nums text-[10px] text-zinc-500 shrink-0">
          {{ elapsedDisplay }} / {{ totalDisplay }}
        </div>
      </div>

      <!-- Timeline bar with row segments -->
      <div class="px-4 pt-1.5 pb-0.5">
        <div
            class="relative h-1.5 w-full rounded-full bg-zinc-800 cursor-pointer"
            @click="seekFromClick"
        >
          <!-- Row segments -->
          <div
              v-for="row in player.rows.value"
              :key="row.id"
              class="absolute inset-y-0 rounded-full transition-colors cursor-pointer"
              :style="{ left: rowPct(row.startTime), width: rowPct(row.duration) }"
              :class="player.activeRow.value?.id === row.id
                  ? 'bg-blue-500/30'
                  : player.elapsed.value >= row.startTime + row.duration && row.duration > 0
                      ? 'bg-zinc-700'
                      : 'bg-zinc-700/50 hover:bg-zinc-600/60'"
              :title="`${row.name} — ${formatTime(row.startTime)} (${formatTime(row.duration)})`"
              @click.stop="player.seekTo(row.startTime)"
          />

          <!-- Row boundary markers -->
          <div
              v-for="row in player.rows.value"
              :key="'m-' + row.id"
              class="absolute top-0 bottom-0 w-px pointer-events-none"
              :class="player.activeRow.value?.id === row.id ? 'bg-blue-400/60' : 'bg-zinc-500/40'"
              :style="{ left: rowPct(row.startTime) }"
          />

          <!-- Elapsed fill -->
          <div
              class="absolute inset-y-0 left-0 rounded-full pointer-events-none transition-[width] duration-75"
              :class="player.isIdle.value ? 'opacity-0' : 'opacity-100'"
              :style="{ width: progressPct }"
          >
            <div class="absolute inset-0 rounded-full bg-blue-500/80" />
          </div>

          <!-- Playhead dot -->
          <div
              v-if="!player.isIdle.value"
              class="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 h-3 w-3 rounded-full bg-bg border-2 border-blue-500 shadow pointer-events-none transition-[left] duration-75"
              :style="{ left: progressPct }"
          />
        </div>
      </div>

      <!-- Row labels under timeline -->
      <div v-if="player.rows.value.length > 0" class="relative h-3.5 mx-4 mb-0.5">
        <button
            v-for="row in player.rows.value"
            :key="'l-' + row.id"
            class="absolute text-[8px] font-mono truncate leading-none transition-colors px-0.5 top-0.5"
            :class="player.activeRow.value?.id === row.id ? 'text-blue-400' : 'text-zinc-600 hover:text-zinc-400'"
            :style="{ left: rowPct(row.startTime), maxWidth: rowPct(row.duration) }"
            :title="row.name"
            @click="player.seekTo(row.startTime)"
        >{{ row.name }}</button>
      </div>

      <!-- Transport controls — compact row -->
      <div class="flex items-center justify-center gap-0.5 px-4 pb-2.5 pt-0.5">
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
                : 'bg-blue-600 text-white hover:bg-blue-500'"
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
    </div>
  </Teleport>
</template>