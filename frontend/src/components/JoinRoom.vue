<template>
  <div class="p-6 max-w-md mx-auto">
    <h1 class="text-2xl font-bold mb-4">Join a Skribbl Room</h1>

    <input v-model="username" placeholder="Enter name" class="border p-2 mb-2 w-full" />
    <input v-model="roomId" placeholder="Enter Room ID" class="border p-2 mb-2 w-full" />
    
    <button @click="joinGame" class="bg-blue-600 text-white px-4 py-2 w-full rounded">
      Join Room
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { connectWebSocket, getStompClient } from '../ws/stompClient.js';

const username = ref('');
const roomId = ref('');
const router = useRouter();

const joinGame = () => {
  if (!username.value || !roomId.value) return alert('Name and Room are required');

  connectWebSocket(() => {
    const client = getStompClient();
    client.publish({
      destination: '/app/join',
      body: JSON.stringify({ username: username.value, roomId: roomId.value })
    });

    router.push({
      name: 'GameRoom',
      query: { username: username.value, roomId: roomId.value }
    });
  });
};
</script>
