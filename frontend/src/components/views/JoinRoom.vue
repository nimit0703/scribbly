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
          <div
            class="mt-6 p-4 bg-notion-gray-dark rounded border border-transparent hover:border-notion-border transition-all duration-200">
            <h3 class="font-medium text-notion-text-primary mb-3 flex items-center text-sm">
              <svg class="w-4 h-4 mr-2 text-notion-text-muted" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                  clip-rule="evenodd" />
              </svg>
              How to Play
            </h3>
            <ul class="text-notion-text-secondary text-sm space-y-2">
              <li class="flex items-start">
                <span class="text-notion-text-muted mr-2">•</span>
                <span>Draw the word when it's your turn</span>
              </li>
              <li class="flex items-start">
                <span class="text-notion-text-muted mr-2">•</span>
                <span>Guess what others are drawing</span>
              </li>
              <li class="flex items-start">
                <span class="text-notion-text-muted mr-2">•</span>
                <span>Earn points for correct guesses</span>
              </li>
              <li class="flex items-start">
                <span class="text-notion-text-muted mr-2">•</span>
                <span>Have fun with friends!</span>
              </li>
            </ul>
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
import { ref, reactive, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { connectWebSocket, getStompClient } from '../../ws/stompClient.js';
import BaseInput from '../ui/BaseInput.vue';
import BaseButton from '../ui/BaseButton.vue';

const username = ref('');
const roomId = ref('');
const isConnecting = ref(false);
const showSuccessToast = ref(false);
const router = useRouter();

const errors = reactive({
  username: '',
  roomId: ''
});

const randomNames = [
  'ArtistAce', 'DoodleDash', 'SketchStar', 'PaintPro', 'DrawMaster',
  'ColorKing', 'BrushBoss', 'InkHero', 'LineLeader', 'ShadeShark',
  'PixelPicasso', 'CanvasKing', 'SketchSavant', 'DoodleDynamo', 'InkWizard'
];

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