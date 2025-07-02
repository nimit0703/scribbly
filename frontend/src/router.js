import { createRouter, createWebHistory } from 'vue-router';
import JoinRoom from './components/JoinRoom.vue';
import GameRoom from './components/GameRoom.vue';

const routes = [
  { path: '/', name: 'JoinRoom', component: JoinRoom },
  { path: '/game', name: 'GameRoom', component: GameRoom },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
