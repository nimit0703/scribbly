<template>
  <div class="min-h-screen bg-gradient-to-br from-purple-200 to-blue-50 p-2 sm:p-4">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="bg-white rounded-xl shadow-lg p-4 mb-4">
        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/>
              </svg>
            </div>
            <div>
              <h1 class="text-xl sm:text-2xl font-bold text-gray-800">{{ username }}</h1>
              <p class="text-sm text-gray-600">Room: <span class="font-semibold text-purple-600">{{ roomId }}</span></p>
            </div>
          </div>

          <div class="flex items-center gap-2 text-sm text-gray-600">
            <h1 class="text-xl sm:text-2xl  text-gray-800" :class="true? 'text-red-700': 'text-gray-800'">{{timer}}sec</h1>
          </div>
          <div class="flex items-center gap-2 text-sm text-gray-600">
            <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span>{{ players.length }} players online</span>
          </div>
        </div>
      </div>

      <!-- Main Game Area -->
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
        <!-- Drawing Canvas -->
        <div class="lg:col-span-3 order-2 lg:order-1">
          <div class="bg-white rounded-xl shadow-lg overflow-hidden">
            <!-- Canvas Header -->
            <div class="bg-gradient-to-r from-indigo-500 to-purple-500 p-4">
              <div class="flex flex-col sm:flex-row justify-between items-center gap-3">
                <div class="text-white">
                  <h2 class="text-lg font-semibold">Drawing Canvas</h2>
                  <p class="text-sm opacity-90">{{ isDrawer ? 'You are drawing!' : 'Guess what\'s being drawn' }}</p>
                </div>
                
                <!-- Control Buttons -->
                <div class="flex gap-2">
                  <div v-if="isDrawer && !selectedWord" class="animate-bounce">
                    <button @click="startRound" 
                            class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 shadow-lg">
                      <div class="flex items-center gap-2">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1.01M15 10h1.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                        Start Round
                      </div>
                    </button>
                  </div>

                  <div v-if="isDrawer && selectedWord">
                    <button @click="endRound" 
                            class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 shadow-lg">
                      <div class="flex items-center gap-2">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 9l3 3l3-3"/>
                        </svg>
                        End Round
                      </div>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Hint Section -->
            <div v-if="hint" class="bg-gradient-to-r from-yellow-100 to-orange-100 p-4 border-b">
              <div class="flex items-center justify-center gap-3">
                <svg class="w-5 h-5 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
                </svg>
                <span class="text-lg font-semibold text-gray-800">Hint: {{ hint }}</span>
              </div>
            </div>

            <!-- Canvas Container -->
            <div class="relative bg-gray-50">
              <CanvasBoard :class="{ 
                'cursor-crosshair': isDrawer && !isEraser,
                'cursor-grab': isDrawer && isEraser,
                'cursor-default': !isDrawer
                }"
              :client="client" 
              :roomId="roomId" 
              :username="username" 
              :isDrawer="isDrawer"/>
              
              <!-- Word Selection Modal -->
              <div v-if="isDrawer && wordOptions.length > 0"
                   class="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-10">
                <div class="bg-white rounded-xl p-6 max-w-md w-full transform animate-pulse">
                  <h3 class="text-xl font-bold text-center mb-4 text-gray-800">Choose a word to draw:</h3>
                  <div class="grid grid-cols-1 gap-3">
                    <button
                      v-for="word in wordOptions"
                      :key="word"
                      @click="selectWord(word)"
                      class="bg-gradient-to-r from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600 text-white px-4 py-3 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 shadow-lg"
                    >
                      {{ word }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="lg:col-span-1 order-1 lg:order-2 space-y-4">
          <!-- Players Panel -->
          <div class="bg-white rounded-xl shadow-lg overflow-hidden">
            <div class="bg-gradient-to-r from-indigo-500 to-purple-500 p-4">
              <h2 class="text-lg font-semibold text-white flex items-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
                </svg>
                Players
              </h2>
            </div>
            <div class="p-4 max-h-48 overflow-y-auto">
              <div v-if="players.length === 0" class="text-center text-gray-500 py-4">
                <svg class="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
                </svg>
                <p>No players yet</p>
              </div>
              <div v-else class="space-y-2">
                <div v-for="(player, index) in players" :key="player.username"
                     class="flex items-center justify-between p-3 rounded-lg transition-all duration-200"
                     :class="player.username === username ? 'bg-gradient-to-r from-purple-100 to-pink-100 border-2 border-purple-300' : 'bg-gray-50 hover:bg-gray-100'">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm"
                         :class="`bg-gradient-to-r ${getPlayerColor(index)}`">
                      {{ player.username.charAt(0).toUpperCase() }}
                    </div>
                    <div>
                      <div class="font-semibold text-gray-800">
                        {{ player.username }}
                        <span v-if="player.username === username" class="text-xs text-purple-600">(You)</span>
                      </div>
                    </div>
                  </div>
                  <div class="text-right">
                    <div class="text-lg font-bold text-gray-800">{{ player.score }}</div>
                    <div class="text-xs text-gray-500">pts</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Chat Panel -->
          <div class="bg-white rounded-xl shadow-lg overflow-hidden">
            <div class="bg-gradient-to-r from-green-500 to-teal-500 p-4">
              <h2 class="text-lg font-semibold text-white flex items-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                </svg>
                Chat & Guesses
              </h2>
            </div>
            <div class="flex flex-col h-80">
              <!-- Chat Messages -->
              <div class="flex-1 overflow-y-auto p-4 space-y-2">
                <div v-if="chat.length === 0" class="text-center text-gray-500 py-8">
                  <svg class="w-12 h-12 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
                  </svg>
                  <p>No messages yet</p>
                  <p class="text-xs">Start chatting!</p>
                </div>
                <div v-for="(msg, index) in chat" :key="msg.timestamp || msg.content + index"
                     class="transition-all duration-300 ease-in-out"
                     :class="msg.username === 'System' ? 'animate-pulse' : 'animate-fadeIn'">
                  <div v-if="msg.username === 'System'"
                       class="bg-gradient-to-r from-yellow-100 to-orange-100 p-2 rounded-lg text-center">
                    <span class="text-sm font-semibold text-orange-800">🎮 {{ msg.content }}</span>
                  </div>
                  <div v-else class="bg-gray-50 rounded-lg p-3">
                    <div class="flex items-start gap-2">
                      <div class="w-6 h-6 rounded-full bg-gradient-to-r from-blue-400 to-purple-400 flex items-center justify-center text-white text-xs font-bold">
                        {{ msg.username.charAt(0).toUpperCase() }}
                      </div>
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-1">
                          <span class="font-semibold text-gray-800 text-sm">{{ msg.username }}</span>
                          <span v-if="msg.username === username" class="text-xs text-purple-600">(You)</span>
                        </div>
                        <p class="text-gray-700 text-sm">{{ msg.content }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Chat Input -->
              <div class="p-4 border-t bg-gray-50">
                <div class="flex gap-2">
                  <input
                    v-model="message"
                    @keyup.enter="sendMessage"
                    placeholder="Type your guess..."
                    class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all duration-200"
                  />
                  <button @click="sendMessage" 
                          :disabled="!message.trim()"
                          class="bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 disabled:from-gray-300 disabled:to-gray-300 text-white px-4 py-2 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 disabled:transform-none shadow-lg">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
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
const isDrawer = ref(false);
const wordOptions = ref([]);
const selectedWord = ref('');
const timer = ref(0);
// Player color themes
const playerColors = [
  'from-red-400 to-pink-400',
  'from-blue-400 to-indigo-400',
  'from-green-400 to-teal-400',
  'from-yellow-400 to-orange-400',
  'from-purple-400 to-pink-400',
  'from-indigo-400 to-purple-400',
  'from-teal-400 to-cyan-400',
  'from-orange-400 to-red-400'
];

const getPlayerColor = (index) => {
  return playerColors[index % playerColors.length];
};

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
      const newMessage = JSON.parse(msg.body);
      chat.value.push(newMessage);
      // Auto scroll to bottom
      setTimeout(() => {
        const chatContainer = document.querySelector('.overflow-y-auto');
        if (chatContainer) {
          chatContainer.scrollTop = chatContainer.scrollHeight;
        }
      }, 100);
    });

    // Subscribe to system messages
    client.value.subscribe(`/topic/system/${roomId}`, (msg) => {
      const systemMessage = { username: 'System', content: msg.body };
      chat.value.push(systemMessage);
      // Auto scroll to bottom
      setTimeout(() => {
        const chatContainer = document.querySelector('.overflow-y-auto');
        if (chatContainer) {
          chatContainer.scrollTop = chatContainer.scrollHeight;
        }
      }, 100);
    });

    // Subscribe to hint
    client.value.subscribe(`/topic/hint/${roomId}`, (msg) => {
      hint.value = msg.body;
    });

    // Subscribe to word options (drawer only)
    client.value.subscribe(`/topic/word-options/${username}`, (msg) => {
      wordOptions.value = JSON.parse(msg.body);
    });

    client.value.subscribe(`/topic/timer/${roomId}`,(msg)=>{
      const remainingTime = JSON.parse(msg.body);
      timer.value = remainingTime;
      
    })
    client.value.subscribe('/topic/timer-ended/' + roomId, function(message) {
      // Handle timer expiration
      console.log("Game over");
      
      // showTimeUpMessage();
  });
    // Publish join again (optional but safe)
    // client.value.publish({
    //   destination: '/app/join',
    //   body: JSON.stringify({ username, roomId })
    // });
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

<style scoped>
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fadeIn {
  animation: fadeIn 0.3s ease-out;
}
</style>