import { createRouter, createWebHistory } from "vue-router";

import Index from "../dashboard/index.vue"

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", component: Index },
    ],
});