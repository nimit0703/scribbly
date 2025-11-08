<!-- src/views/JoinGame.vue -->
<template>
  <div class="min-h-screen flex items-center justify-center p-4 font-inter bg-notion-dark">
    <div class="w-full max-w-md">
      <!-- Main Card -->
      <div class="bg-notion-gray-darker  rounded-lg hover:shadow-2xl transition-all duration-200">
        <!-- Header -->
        <div class="p-8 text-center relative">
          <div
            class="inline-flex items-center justify-center w-14 h-14 bg-notion-gray-dark rounded-full mb-4 border border-notion-border">
            <svg class="w-7 h-7 text-notion-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
            </svg>
          </div>
          <h1 class="text-2xl font-semibold text-notion-text-primary mb-1">Scribble Game</h1>
          <p class="text-notion-text-secondary">Draw & Guess with Friends</p>
        </div>

        <!-- Form Content -->
        <div class="px-8 pb-8">
          <form @submit.prevent="joinGame" class="space-y-6">
            <!-- Username Input -->
            <div class="space-y-2">
              <BaseInput id="username" label="Your Name" v-model="username" placeholder="Enter your nickname"
                :error="errors.username" @update:modelValue="clearError('username')">
                <template #icon>
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                </template>
              </BaseInput>
            </div>

            <!-- Room ID Input -->
            <div class="space-y-2">
              <BaseInput id="roomId" label="Room ID" v-model="roomId" placeholder="Enter room code"
                :error="errors.roomId" @update:modelValue="clearError('roomId')">
                <template #icon>
                  <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                </template>
              </BaseInput>
            </div>

            <!-- Join Button -->
            <BaseButton variant="primary" size="lg" @click="joinGame" :disabled="isConnecting" class="w-full">
              {{ isConnecting ? 'Joining...' : 'Join Room' }}
              <template #icon-right>
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                </svg>
              </template>
            </BaseButton>
          </form>

          <!-- Quick Actions -->
          <div class="mt-8 pt-6 border-t border-notion-border">
            <div class="flex flex-col sm:flex-row gap-3">

              <BaseButton variant="secondary" size="md" @click="generateRandomRoom" class="w-full">
                Random Room
                <template #icon-right>
                  <i data-feather="message-square" class="w-4 h-4 text-notion-text-muted"></i>
                </template>
              </BaseButton>
              <BaseButton variant="secondary" size="md" @click="generateRandomName" class="w-full">
                Random Name
                <template #icon-right>
                  <i data-feather="user" class="w-4 h-4 text-notion-text-muted"></i>
                </template>
              </BaseButton>
            </div>
          </div>

          <!-- Game Rules -->
          <div class="mt-6 p-4 mx-auto flex overflow-hidden relative min-h-64">
            <!-- Random SVG elements will be positioned here -->
            <div v-for="(svg, index) in svgElements" :key="index" class="absolute" :style="{
              left: `${svg.position.x}%`,
              top: `${svg.position.y}%`,
              transform: `rotate(${svg.rotation}deg) scale(${svg.scale})`,
              opacity: svg.opacity,
              zIndex: svg.zIndex,
              margin: '10px'
            }">
              <SVGThinking v-if="svg.component === 'SVGThinking'" :class="`h-${svg.size} ${svg.color}`" />
              <SVGThinking2 v-else-if="svg.component === 'SVGThinking2'" :class="`h-${svg.size} ${svg.color}`" />
              <Squirrel v-else-if="svg.component === 'Squirrel'" :class="`h-${svg.size} ${svg.color}`" />
              <Crocodile v-else-if="svg.component === 'Crocodile'" :class="`h-${svg.size} ${svg.color}`" />
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="text-center mt-6 text-notion-text-muted text-sm">
        <p>Create awesome drawings with friends! ✨</p>
      </div>
    </div>

    <!-- Success Toast -->
    <transition name="toast">
      <div v-if="showSuccessToast" class="fixed top-4 right-4 bg-green-600 text-white px-4 py-2 rounded shadow-lg z-50">
        <div class="flex items-center space-x-2">
          <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              clip-rule="evenodd" />
          </svg>
          <span class="text-sm">Joining room...</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { connectWebSocket, getStompClient } from '../../ws/stompClient.js';
import BaseInput from '../ui/BaseInput.vue';
import BaseButton from '../ui/BaseButton.vue';
import SVGThinking from '../ui/SVG/SVGThinking.vue';
import SVGThinking2 from '../ui/SVG/SVGThinking2.vue';
import Squirrel from '../ui/SVG/Squirrel.vue';
import Crocodile from '../ui/SVG/Crocodile.vue';

const username = ref('');
const roomId = ref('');
const isConnecting = ref(false);
const showSuccessToast = ref(false);
const router = useRouter();

const errors = reactive({
  username: '',
  roomId: ''
});

const svgElements = ref([])
const svgComponents = ref(['SVGThinking', 'SVGThinking2', 'Squirrel', 'Crocodile'])
const colors = ref([
  'text-blue-400',
  'text-purple-400',
  'text-indigo-500',
  'text-pink-400',
  'text-teal-400'
])
const sizes = ref([32, 40, 48])
const randomNames = [
  'ArtistAce', 'DoodleDash', 'SketchStar', 'PaintPro', 'DrawMaster',
  'ColorKing', 'BrushBoss', 'InkHero', 'LineLeader', 'ShadeShark',
  'PixelPicasso', 'CanvasKing', 'SketchSavant', 'DoodleDynamo', 'InkWizard'
];

const generateRandomIllustration = () => {
  svgElements.value = []; // clear existing

  const elementCount = Math.floor(Math.random() * 4) + 4; // 10–14 SVGs
  const minDistance = 12; // minimum spacing (in % of container width/height)

  const isTooClose = (x, y, elements) => {
    return elements.some(el => {
      const dx = el.position.x - x;
      const dy = el.position.y - y;
      const distance = Math.sqrt(dx * dx + dy * dy);
      return distance < minDistance;
    });
  };

  for (let i = 0; i < elementCount; i++) {
    let position;
    let attempts = 0;

    do {
      position = {
        x: Math.random() * 65, // keep margin
        y: Math.random() * 65
      };
      attempts++;
      // Prevent infinite loops if too crowded
      if (attempts > 100) break;
    } while (isTooClose(position.x, position.y, svgElements.value));

    svgElements.value.push({
      component: svgComponents.value[Math.floor(Math.random() * svgComponents.value.length)],
      position,
      rotation: Math.random() * 360,
      scale: (Math.random() * 0.7) + 0.7,
      opacity: 1,
      size: sizes.value[Math.floor(Math.random() * sizes.value.length)],
      color: colors.value[Math.floor(Math.random() * colors.value.length)],
      zIndex: 10 + Math.floor(Math.random() * 10)
    });
  }
};


onMounted(() => {
  setInterval(() => {
    generateRandomIllustration()
  }, 3000);
})
const generateRandomRoom = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < 6; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  roomId.value = result;
  clearError('roomId');
};

const generateRandomName = () => {
  const randomIndex = Math.floor(Math.random() * randomNames.length);
  username.value = randomNames[randomIndex] + Math.floor(Math.random() * 100);
  clearError('username');
};

const validateForm = () => {
  let isValid = true;
  errors.username = '';
  errors.roomId = '';

  if (!username.value.trim()) {
    errors.username = 'Name is required';
    isValid = false;
  } else if (username.value.trim().length < 2) {
    errors.username = 'Name must be at least 2 characters';
    isValid = false;
  } else if (username.value.trim().length > 20) {
    errors.username = 'Name must be less than 20 characters';
    isValid = false;
  }

  if (!roomId.value.trim()) {
    errors.roomId = 'Room ID is required';
    isValid = false;
  } else if (roomId.value.trim().length < 3) {
    errors.roomId = 'Room ID must be at least 3 characters';
    isValid = false;
  }

  return isValid;
};

const clearError = (field) => {
  errors[field] = '';
};

const joinGame = async () => {
  if (!validateForm()) return;
  if (isConnecting.value) return;

  try {
    isConnecting.value = true;
    showSuccessToast.value = true;

    await connectWebSocket(() => {
      const client = getStompClient();
      client.publish({
        destination: '/app/join',
        body: JSON.stringify({
          username: username.value.trim(),
          roomId: roomId.value.trim().toUpperCase()
        })
      });

      router.push({
        name: 'GameRoom',
        query: {
          username: username.value.trim(),
          roomId: roomId.value.trim().toUpperCase()
        }
      });
    });

  } catch (error) {
    console.error('Failed to join room:', error);
    errors.roomId = 'Failed to connect. Please try again.';
  } finally {
    isConnecting.value = false;
    setTimeout(() => {
      showSuccessToast.value = false;
    }, 2000);
  }
};

nextTick(() => {
  const usernameInput = document.getElementById('username');
  if (usernameInput) {
    usernameInput.focus();
  }
});
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
</style>