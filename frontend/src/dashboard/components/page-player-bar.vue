<script setup lang="ts">
import { computed } from "vue"
import type { usePagePlayer } from "../usePagePlayer.ts"
import { formatTime } from "../useTimeFormat.ts"

type Player = ReturnType<typeof usePagePlayer>

const props = defineProps<{ player: Player }>()

// ── Display helpers ───────────────────────────────────────────────────────────

const pageName = computed(() => props.player.page.value?.name ?? null)

const elapsedDisplay  = computed(() => formatTime(Math.floor(props.player.elapsed.value)))
const totalDisplay    = computed(() => formatTime(props.player.totalDuration.value))

const remainingDisplay = computed(() => {
  const rem = Math.max(0, props.player.totalDuration.value - props.player.elapsed.value)
  return "-" + formatTime(Math.floor(rem))
})

const activeRowName = computed(() => props.player.activeRow.value?.name ?? null)

const progressPct = computed(() => `${(props.player.progress.value * 100).toFixed(2)}%`)

const progressColor = computed(() => {
  const p = props.player.progress.value
  if (p >= 0.9) return "bg-red-500"
  if (p >= 0.7) return "bg-amber-400"
  return "bg-emerald-500"
})


</script>

<template>
  <Teleport to="body">
    <div
        class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50
                   flex flex-col items-stretch
                   rounded-xl border border-zinc-700/80 bg-zinc-900/95 backdrop-blur-md shadow-2xl
                   min-w-[520px] max-w-[700px] w-auto select-none overflow-hidden"
    >
      <!-- Timeline progress bar — clickable to seek -->
      <div
          class="relative h-1 w-full bg-zinc-800 cursor-pointer group/bar"
          @click="(e) => {
                    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
                    const pct  = (e.clientX - rect.left) / rect.width
                    player.seekTo(Math.round(pct * player.totalDuration.value))
                }"
      >
        <!-- Elapsed fill -->
        <div
            class="absolute inset-y-0 left-0 transition-all duration-75 pointer-events-none"
            :class="[progressColor, player.isIdle.value ? 'opacity-0' : 'opacity-100']"
            :style="{ width: progressPct }"
        />
        <!-- Row markers -->
        <div
            v-for="row in player.rows.value"
            :key="row.id"
            class="absolute top-0 bottom-0 w-px bg-zinc-600 pointer-events-none"
            :style="{ left: `${(row.startTime / player.totalDuration.value) * 100}%` }"
        />
        <!-- Hover expand affordance -->
        <div class="absolute inset-0 group-hover/bar:bg-white/5 transition-colors pointer-events-none" />
      </div>

      <!-- Main transport row -->
      <div class="flex items-center gap-3 px-4 py-2.5">

        <!-- Page name + active row label -->
        <div class="flex flex-col min-w-0 flex-1">
                    <span
                        v-if="pageName"
                        class="text-[10px] font-mono text-zinc-500 uppercase tracking-widest truncate leading-tight"
                    >{{ pageName }}</span>
          <span
              class="text-xs font-mono truncate leading-snug transition-colors"
              :class="activeRowName ? 'text-zinc-200' : 'text-zinc-600 italic'"
          >{{ activeRowName ?? "—" }}</span>
        </div>

        <!-- Time -->
        <div class="flex items-center gap-1 font-mono tabular-nums text-xs shrink-0">
          <span class="text-zinc-300">{{ elapsedDisplay }}</span>
          <span class="text-zinc-700">/</span>
          <span class="text-zinc-500">{{ totalDisplay }}</span>
          <span
              v-if="player.totalDuration.value && !player.isIdle.value"
              class="ml-1 text-zinc-600"
          >{{ remainingDisplay }}</span>
        </div>

        <!-- Transport controls -->
        <div class="flex items-center gap-1 shrink-0">

          <!-- Play / Pause -->
          <button
              class="p-2 rounded-lg transition-colors"
              :class="player.isPlaying.value
                            ? 'bg-zinc-700 text-zinc-100 hover:bg-zinc-600'
                            : 'bg-emerald-600 text-white hover:bg-emerald-500'"
              :disabled="!player.page.value"
              :title="player.isPlaying.value ? 'Pause' : 'Play'"
              @click="player.togglePlay()"
          >
            <svg v-if="player.isPlaying.value" viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
              <rect x="3"  y="3" width="4" height="10" rx="1"/>
              <rect x="9" y="3" width="4" height="10" rx="1"/>
            </svg>
            <svg v-else viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
              <path d="M4 3l10 5-10 5V3z"/>
            </svg>
          </button>

          <!-- Stop -->
          <button
              class="p-1.5 rounded-md text-zinc-500 hover:text-red-400 hover:bg-zinc-800
                               disabled:opacity-25 disabled:pointer-events-none transition-colors"
              :disabled="player.isIdle.value"
              title="Stop and clear all"
              @click="player.stop()"
          >
            <svg viewBox="0 0 16 16" width="16" height="16" fill="currentColor">
              <rect x="3" y="3" width="10" height="10" rx="1.5"/>
            </svg>
          </button>

        </div>

        <!-- On-air indicator -->
        <div
            v-if="player.activeRow.value"
            class="flex items-center gap-1.5 shrink-0 px-2 py-1 rounded bg-red-950/60 border border-red-800/50"
        >
          <span class="h-1.5 w-1.5 rounded-full bg-red-500 animate-pulse" />
          <span class="text-[10px] font-mono text-red-400 uppercase tracking-widest">Live</span>
        </div>

      </div>

      <!-- Row strip — absolutely positioned, left = startTime, width = duration -->
      <div
          v-if="player.rows.value.length > 0 && !player.isIdle.value"
          class="relative h-1.5 mx-4 mb-3 bg-zinc-800 rounded-full"
      >
        <button
            v-for="row in player.rows.value"
            :key="row.id"
            class="absolute inset-y-0 rounded-full transition-colors"
            :style="{
              left:  `${(row.startTime / player.totalDuration.value) * 100}%`,
              width: `${(row.duration  / player.totalDuration.value) * 100}%`,
            }"
            :class="player.activeRow.value?.id === row.id
                        ? 'bg-emerald-500'
                        : player.elapsed.value >= row.startTime + row.duration && row.duration > 0
                            ? 'bg-zinc-600'
                            : 'bg-zinc-700 hover:bg-zinc-500'"
            :title="`${row.name}  ${formatTime(row.startTime)}  (${formatTime(row.duration)})`"
            @click="player.jumpToRow(player.rows.value.indexOf(row))"
        />
      </div>
    </div>
  </Teleport>
</template>