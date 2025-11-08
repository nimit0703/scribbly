import { createRouter, createWebHistory } from 'vue-router';
import JoinRoom from './components/views/JoinRoom.vue';
import GameRoom from './components/views/GameRoom.vue';

const routes = [
  { path: '/', name: 'JoinRoom', component: JoinRoom },
  { path: '/game', name: 'GameRoom', component: GameRoom },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
