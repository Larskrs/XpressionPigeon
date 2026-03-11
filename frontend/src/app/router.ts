import { createRouter, createWebHistory } from "vue-router";

import Dashboard from "../Dashboard.vue"

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", component: Dashboard },
    ],
});