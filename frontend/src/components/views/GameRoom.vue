<!-- src/views/GameRoom.vue -->
<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-100 to-primary-50 p-2 sm:p-4">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <BaseCard class="mb-4">
        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-primary-700 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
              </svg>
            </div>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold text-primary-900">{{ username }}</h1>
              <p class="text-sm text-primary-600">Room: <span class="font-semibold text-primary-700">{{ roomId }}</span>
              </p>
            </div>
          </div>

          <div class="flex items-center gap-4">
            <!-- Timer -->
            <div class="flex items-center gap-2">
              <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h1 class="text-xl font-bold" :class="timer < 10 ? 'text-red-600' : 'text-primary-800'">
                {{ timer }}s
              </h1>
            </div>

            <!-- Players Count -->
            <div class="flex items-center gap-2 text-sm text-primary-600">
              <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span>{{ players.length }} players</span>
            </div>
          </div>
        </div>
      </BaseCard>

      <!-- Main Game Area -->
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <!-- Drawing Canvas -->
        <div class="lg:col-span-3 order-2 lg:order-1">
          <BaseCard noPadding>
            <!-- Canvas Header -->
            <div class="bg-primary-700 p-4">
              <div class="flex flex-col sm:flex-row justify-between items-center gap-3">
                <div class="text-white">
                  <h2 class="text-lg font-semibold">Drawing Canvas</h2>
                  <p class="text-sm text-primary-200">{{ isDrawer ? 'You are drawing!' : 'Guess what\'s being drawn' }}
                  </p>
                </div>

                <!-- Control Buttons -->
                <div class="flex gap-2">
                  <BaseButton v-if="isDrawer && !selectedWord" variant="accent" @click="startRound"
                    class="animate-bounce">
                    <template #icon-left>
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1.01M15 10h1.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </template>
                    Start Round
                  </BaseButton>

                  <BaseButton v-if="isDrawer && selectedWord" variant="danger" @click="endRound">
                    <template #icon-left>
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9l3 3l3-3" />
                      </svg>
                    </template>
                    End Round
                  </BaseButton>
                </div>
              </div>
            </div>

            <!-- Hint Section -->
            <div v-if="hint" class="bg-primary-100 p-4 border-b border-primary-200">
              <div class="flex items-center justify-center gap-3">
                <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                </svg>
                <span class="text-base text-primary-800">
                  Hint: <span class="mx-4 tracking-[.25em] font-semibold">{{ hint }}</span>
                </span>
              </div>
            </div>

            <!-- Canvas Container -->
            <div class="relative bg-primary-50">
              <CanvasBoard :client="client" :roomId="roomId" :username="username" :isDrawer="isDrawer" />

              <!-- Word Selection Modal -->
              <div v-if="isDrawer && wordOptions.length > 0"
                class="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-10">
                <div class="bg-white rounded-xl p-6 max-w-md w-full">
                  <h3 class="text-xl font-bold text-center mb-4 text-primary-900">Choose a word to draw:</h3>
                  <div class="grid grid-cols-1 gap-3">
                    <BaseButton v-for="word in wordOptions" :key="word" variant="primary" size="lg"
                      @click="selectWord(word)" class="w-full">
                      {{ word }}
                    </BaseButton>
                  </div>
                </div>
              </div>
            </div>
          </BaseCard>
        </div>

        <!-- Sidebar -->
        <div class="lg:col-span-1 order-1 lg:order-2 space-y-4">
          <!-- Players Panel -->
          <PlayerList :players="players" :currentUsername="username" />

          <!-- Chat Panel -->
          <ChatPanel :messages="chat" :currentUsername="username" @send-message="sendMessage" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import BaseCard from '../ui/BaseCard.vue';
import BaseButton from '../ui/BaseButton.vue';
import PlayerList from '../game/PlayerList.vue';
import ChatPanel from '../game/ChatPanel.vue';
import CanvasBoard from '../game/CanvasBoard.vue';
import { connectWebSocket, getStompClient } from '../../ws/stompClient';

const route = useRoute();
const username = route.query.username;
const roomId = route.query.roomId;

const client = ref(null);
const players = ref([]);
const chat = ref([]);
const hint = ref('');
const isDrawer = ref(false);
const wordOptions = ref([]);
const selectedWord = ref('');
const timer = ref(0);

onMounted(() => {
  connectWebSocket(() => {
    client.value = getStompClient();

    // Subscribe to player list updates
    client.value.subscribe(`/topic/players/${roomId}`, (msg) => {
      players.value = JSON.parse(msg.body);
    });

    // Subscribe to drawer updates
    client.value.subscribe(`/topic/drawer/${roomId}`, (msg) => {
      const drawerUsername = msg.body;
      isDrawer.value = (drawerUsername === username);
    });

    // Subscribe to public chat
    client.value.subscribe(`/topic/chat/${roomId}`, (msg) => {
      const newMessage = JSON.parse(msg.body);
      chat.value.push(newMessage);
    });

    // Subscribe to system messages
    client.value.subscribe(`/topic/system/${roomId}`, (msg) => {
      const systemMessage = { username: 'System', content: msg.body };
      chat.value.push(systemMessage);
    });

    // Subscribe to hint
    client.value.subscribe(`/topic/hint/${roomId}`, (msg) => {
      hint.value = msg.body;
    });

    // Subscribe to word options (drawer only)
    client.value.subscribe(`/topic/word-options/${username}`, (msg) => {
      wordOptions.value = JSON.parse(msg.body);
    });

    // Subscribe to timer
    client.value.subscribe(`/topic/timer/${roomId}`, (msg) => {
      timer.value = JSON.parse(msg.body);
    });

    // Subscribe to timer ended
    client.value.subscribe(`/topic/timer-ended/${roomId}`, () => {
      console.log("Game over");
    });
  });
});

const sendMessage = (content) => {
  client.value.publish({
    destination: '/app/chat',
    body: JSON.stringify({
      roomId,
      username,
      content
    })
  });
};

const startRound = () => {
  client.value.publish({
    destination: '/app/start-round',
    body: JSON.stringify({ roomId })
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