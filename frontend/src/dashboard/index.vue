<!-- index.vue -->
<script setup lang="ts">
import { onBeforeUnmount, onMounted } from "vue"
import { createSocket } from "./socket.ts"
import { useSocket } from "./useSocket.ts"
import { groups, useScenes } from "./useScenes.ts"
import SceneGroupComponent from "./components/scene-group.vue"

const socket = createSocket()
useSocket(socket)

onMounted(() => socket.connect())
onBeforeUnmount(() => socket.disconnect())

const { scenes, sceneStates, takeScene, outScene, updateField, handleFlip } = useScenes(socket)
</script>

<template>
  <nav class="flex flex-wrap gap-6 px-4 py-3 border-b border-zinc-800 bg-zinc-900">
    <SceneGroupComponent
        v-for="group in groups"
        :key="group.label"
        :group="group"
        :scenes="scenes"
        :scene-states="sceneStates"
        @take="takeScene"
        @out="outScene"
        @update="updateField"
        @flip="handleFlip"
    />
  </nav>
  <main></main>
</template>