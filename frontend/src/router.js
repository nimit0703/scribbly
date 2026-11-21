import { createRouter, createWebHistory } from 'vue-router';
import HomeView from './views/HomeView.vue';
import PlayView from './views/PlayView.vue';
import { store } from './game/store.js';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    // Only reachable after joining; a refresh drops the connection, so send people back home.
    { path: '/play', name: 'play', component: PlayView, beforeEnter: () => (store.room ? true : { name: 'home' }) },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

export default router;
