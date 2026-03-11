import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import {fileURLToPath} from "node:url";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [
      vue(),
      tailwindcss(),
  ],
  server: {
    proxy: {
      // REST
      "/api": "http://localhost:80",

      // WebSockets (adjust path to match yours)
      "/ws": {
        target: "ws://localhost:80",
        ws: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
});