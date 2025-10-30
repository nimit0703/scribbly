<!-- src/views/JoinGame.vue -->
<template>
  <div
    class="min-h-screen bg-gradient-to-br from-primary-100 via-primary-50 to-accent-50 flex items-center justify-center p-4">
    <div class="w-full max-w-lg">
      <!-- Main Card -->
      <BaseCard noPadding>
        <!-- Header -->
        <div class="bg-primary-800 p-8 text-center">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-white bg-opacity-10 rounded-full mb-4">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-white mb-2">Skribbl Room</h1>
          <p class="text-primary-200 text-lg">Draw, guess, and have fun!</p>
        </div>

        <!-- Form Content -->
        <div class="p-8">
          <form @submit.prevent="joinGame" class="space-y-6">
            <!-- Username Input -->
            <BaseInput id="username" v-model="username" label="Your Name" placeholder="Enter your name"
              :error="errors.username" @input="clearError('username')">
              <template #icon>
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </template>
            </BaseInput>

            <!-- Room ID Input -->
            <BaseInput id="roomId" v-model="roomId" label="Room ID" placeholder="Enter Room ID" :error="errors.roomId"
              @input="clearError('roomId')">
              <template #icon>
                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
              </template>
            </BaseInput>

            <!-- Join Button -->
            <BaseButton type="submit" variant="ghost" size="lg" :disabled="isConnecting"
              class="w-full text-black border bg-blue">
              <!-- <template #icon-left v-if="isConnecting">
                <svg class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z">
                  </path>
                </svg>
              </template> -->
              {{ isConnecting ? 'Connecting...' : 'Join Room' }}
            </BaseButton>
          </form>

          <!-- Quick Actions -->
          <div class="mt-8 pt-6 border-t border-primary-200">
            <div class="flex flex-col sm:flex-row gap-3">
              <BaseButton variant="ghost" size="sm" @click="generateRandomRoom" class="flex-1 text-black">
                🎲 Random Room
              </BaseButton>
              <BaseButton variant="ghost" size="sm" @click="generateRandomName" class="flex-1 text-black">
                👤 Random Name
              </BaseButton>
            </div>
          </div>

          <!-- Game Rules -->
          <div class="mt-6 p-4 bg-primary-50 rounded-xl border border-primary-200">
            <h3 class="font-semibold text-primary-900 mb-2 flex items-center text-sm">
              <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                  clip-rule="evenodd" />
              </svg>
              How to Play
            </h3>
            <ul class="text-primary-700 text-sm space-y-1">
              <li>• Draw the word when it's your turn</li>
              <li>• Guess what others are drawing</li>
              <li>• Earn points for correct guesses</li>
              <li>• Have fun with friends!</li>
            </ul>
          </div>
        </div>
      </BaseCard>

      <!-- Footer -->
      <div class="text-center mt-6 text-primary-600 text-sm">
        <p>Create memorable moments with friends 🎨</p>
      </div>
    </div>

    <!-- Success Toast -->
    <transition name="toast">
      <div v-if="showSuccessToast"
        class="fixed top-4 right-4 bg-primary-800 text-white px-6 py-3 rounded-lg shadow-lg z-50">
        <div class="flex items-center space-x-2">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
              clip-rule="evenodd" />
          </svg>
          <span>Joining room...</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { connectWebSocket, getStompClient } from '../../ws/stompClient.js';
import BaseButton from '../ui/BaseButton.vue';
import BaseInput from '../ui/BaseInput.vue';
import BaseCard from '../ui/BaseCard.vue';

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
  'ColorKing', 'BrushBoss', 'InkHero', 'LineLeader', 'ShadeShark'
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
  username.value = randomNames[randomIndex] + Math.floor(Math.random() * 1000);
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