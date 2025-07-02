<template>
  <div class="grid grid-cols-3 gap-4 p-4 h-screen">
    <!-- Drawing Canvas -->
    <div class="col-span-2 border relative">
      <CanvasBoard :client="client" :roomId="roomId" :username="username" />
      <div class="mt-2 text-xl text-center font-bold">Hint: {{ hint }}</div>

      <!-- Start Round Button -->
      <div v-if="isDrawer && !selectedWord" class="absolute top-4 left-4">
        <button @click="startRound" class="bg-green-600 text-white px-4 py-2 rounded">
          Start Round
        </button>
      </div>

      <!-- End Round Button -->
      <div v-if="isDrawer && selectedWord" class="absolute top-4 right-4">
        <button @click="endRound" class="bg-red-600 text-white px-4 py-2 rounded">
          End Round
        </button>
      </div>

      <!-- Word Options -->
      <div v-if="isDrawer && wordOptions.length > 0"
           class="absolute top-20 left-1/2 transform -translate-x-1/2 bg-white border p-4 rounded shadow-md">
        <h3 class="font-bold mb-2">Choose a word:</h3>
        <div class="flex gap-2">
          <button
            v-for="word in wordOptions"
            :key="word"
            @click="selectWord(word)"
            class="bg-blue-500 text-white px-3 py-1 rounded"
            >
            {{ word }}
          </button>
        </div>
      </div>
    </div>

    <!-- Sidebar: Players + Chat -->
    <div class="col-span-1">
      <div class="mb-6">
        <h2 class="font-bold text-lg mb-2">Players</h2>
        <ul>
          <li v-for="player in players" :key="player.username">
            {{ player.username }} - {{ player.score }} pts
          </li>
        </ul>
      </div>

      <div>
        <h2 class="font-bold mb-2">Chat</h2>
        <div class="h-48 overflow-y-auto border p-2 bg-white text-sm">
          <div v-for="msg in chat" :key="msg.timestamp || msg.content">
            <strong>{{ msg.username }}:</strong> {{ msg.content }}
          </div>
        </div>
        <input
          v-model="message"
          @keyup.enter="sendMessage"
          placeholder="Type guess..."
          class="border p-2 w-full mt-2"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import CanvasBoard from './CanvasBoard.vue';
import { connectWebSocket, getStompClient } from '../ws/stompClient';

const route = useRoute();
const username = route.query.username;
const roomId = route.query.roomId;

const client = ref(null);
const players = ref([]);
const chat = ref([]);
const message = ref('');
const hint = ref('');
const isDrawer = ref(false); // temporary — real drawer logic should come from server
const wordOptions = ref([]);
const selectedWord = ref('');

onMounted(() => {
  connectWebSocket(() => {

    client.value = getStompClient();

    // Subscribe to player list updates
    client.value.subscribe(`/topic/players/${roomId}`, (msg) => {
      players.value = JSON.parse(msg.body);
    });
    client.value.subscribe(`/topic/drawer/${roomId}`, (msg) => {
    const drawerUsername = msg.body;
    isDrawer.value = (drawerUsername === username);
    });

    // Subscribe to public chat
    client.value.subscribe(`/topic/chat/${roomId}`, (msg) => {
      chat.value.push(JSON.parse(msg.body));
    });

    // Subscribe to system messages
    client.value.subscribe(`/topic/system/${roomId}`, (msg) => {
      chat.value.push({ username: 'System', content: msg.body });
    });

    // Subscribe to hint
    client.value.subscribe(`/topic/hint/${roomId}`, (msg) => {
      hint.value = msg.body;
    });

    // Subscribe to word options (drawer only)
    client.value.subscribe(`/topic/word-options/${username}`, (msg) => {
    wordOptions.value = JSON.parse(msg.body);
    });

    // Publish join again (optional but safe)
    client.value.publish({
      destination: '/app/join',
      body: JSON.stringify({ username, roomId })
    });
  });
});

const sendMessage = () => {
  if (!message.value.trim()) return;

  client.value.publish({
    destination: '/app/chat',
    body: JSON.stringify({
      roomId,
      username,
      content: message.value
    })
  });

  message.value = '';
};

const startRound = () => {
  client.value.publish({
    destination: '/app/start-round',
    body: JSON.stringify({ roomId }) // ⛔️ remove `drawer`
    });
};

const selectWord = (word) => {
  selectedWord.value = word;
  wordOptions.value = [];

  client.value.publish({
    destination: '/app/word-select',
    body: JSON.stringify({ roomId, drawer: username, word })
  });
};

const endRound = () => {
  client.value.publish({
    destination: '/app/end-round',
    body: JSON.stringify({ roomId })
  });

  selectedWord.value = '';
  hint.value = '';
};
</script>
