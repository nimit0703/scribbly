<!-- src/views/GameRoom.vue -->
<template>
  <div class="min-h-screen  bg-notion-dark p-4">
    <div class=" mx-auto">
      <!-- Header -->
      <BaseCard class="mb-4">
        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-notion-accent rounded-lg flex items-center justify-center">
              <i data-feather="edit-3" class="text-notion-text-primary"></i>
            </div>
            <div>
              <h1 class="text-xl font-semibold text-notion-text-primary">{{ username }}</h1>
              <p class="text-sm text-notion-text-secondary">Room: <span class="font-medium text-notion-text-primary">{{ roomId }}</span></p>
            </div>
          </div>

          <div class="flex items-center gap-4">
            <!-- Timer -->
            <div class="flex items-center gap-2">
              <i data-feather="clock" class="text-notion-text-secondary"></i>
              <h1 class="text-xl font-semibold" :class="timer < 10 ? 'text-notion-accent' : 'text-notion-text-primary'">
                {{ timer }}s
              </h1>
            </div>

            <!-- Players Count -->
            <div class="flex items-center gap-2 text-sm text-notion-text-secondary">
              <div class="w-2 h-2 bg-notion-accent rounded-full"></div>
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
            <div class="bg-notion-gray-dark p-4 border-b border-notion-border">
              <div class="flex flex-col sm:flex-row justify-between items-center gap-3">
                <div class="text-notion-text-primary">
                  <h2 class="text-lg font-semibold">Drawing Canvas</h2>
                  <p class="text-sm text-notion-text-secondary">{{ isDrawer ? 'You are drawing!' : 'Guess what\'s being drawn' }}</p>
                </div>

                <!-- Control Buttons -->
                <div class="flex gap-2">
                  <BaseButton 
                    v-if="isDrawer && !selectedWord" 
                    variant="accent" 
                    @click="startRound"
                    class="flex items-center gap-2"
                  >
                    <i data-feather="play" class="w-4 h-4"></i>
                    Start Round
                  </BaseButton>

                  <BaseButton 
                    v-if="isDrawer && selectedWord" 
                    variant="accent" 
                    @click="endRound"
                    class="flex items-center gap-2"
                  >
                    <i data-feather="square" class="w-4 h-4"></i>
                    End Round
                  </BaseButton>
                </div>
              </div>
            </div>

            <!-- Hint Section -->
            <div v-if="hint" class="bg-notion-gray-darker p-4 border-b border-notion-border">
              <div class="flex items-center justify-center gap-3">
                <i data-feather="help-circle" class="text-notion-text-secondary"></i>
                <span class="text-base text-notion-text-primary">
                  Hint: <span class="mx-4 tracking-[.25em] font-semibold">{{ hint }}</span>
                </span>
              </div>
            </div>

            <!-- Canvas Container -->
            <div class="relative bg-notion-gray-darker">
              <CanvasBoard :client="client" :roomId="roomId" :username="username" :isDrawer="isDrawer" />

              <!-- Word Selection Modal -->
              <div v-if="isDrawer && wordOptions.length > 0" class="absolute inset-0 bg-black bg-opacity-70 flex items-center justify-center p-4 z-10">
                <div class="bg-notion-gray-dark rounded-lg border border-notion-border p-6 max-w-md w-full">
                  <h3 class="text-xl font-semibold text-center mb-4 text-notion-text-primary">Choose a word to draw:</h3>
                  <div class="grid grid-cols-1 gap-3">
                    <BaseButton 
                      v-for="word in wordOptions" 
                      :key="word" 
                      variant="secondary" 
                      size="lg"
                      @click="selectWord(word)" 
                      class="w-full text-center"
                    >
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
import { ref, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import BaseCard from '../ui/BaseCard.vue';
import BaseButton from '../ui/BaseButton.vue';
import PlayerList from '../game/PlayerList.vue';
import ChatPanel from '../game/ChatPanel.vue';
import CanvasBoard from '../game/CanvasBoard.vue';
import { connectWebSocket, getStompClient } from '../../ws/stompClient';

// Initialize Feather icons

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
  // Replace Feather icons
  nextTick(() => {
    feather.replace();
  });
  
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