<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-100 via-purple-50 to-pink-100 flex items-center justify-center p-4">
    <div class="w-full max-w-lg">
      <!-- Main Card -->
      <div class="bg-white rounded-2xl shadow-2xl overflow-hidden">
        <!-- Header -->
        <div class="bg-gradient-to-r from-indigo-600 to-purple-600 p-8 text-center">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-white bg-opacity-20 rounded-full mb-4">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/>
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-white mb-2">Skribbl Room</h1>
          <p class="text-indigo-100 text-lg">Draw, guess, and have fun!</p>
        </div>

        <!-- Form Content -->
        <div class="p-8">
          <form @submit.prevent="joinGame" class="space-y-6">
            <!-- Username Input -->
            <div class="space-y-2">
              <label for="username" class="block text-sm font-semibold text-gray-700">
                Your Name
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                  </svg>
                </div>
                <input 
                  id="username"
                  v-model="username" 
                  type="text"
                  placeholder="Enter your name" 
                  class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all duration-200 bg-gray-50 focus:bg-white"
                  :class="{ 'border-red-300 focus:ring-red-500': errors.username }"
                  @input="clearError('username')"
                />
              </div>
              <p v-if="errors.username" class="text-red-500 text-sm mt-1">{{ errors.username }}</p>
            </div>

            <!-- Room ID Input -->
            <div class="space-y-2">
              <label for="roomId" class="block text-sm font-semibold text-gray-700">
                Room ID
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                  </svg>
                </div>
                <input 
                  id="roomId"
                  v-model="roomId" 
                  type="text"
                  placeholder="Enter Room ID" 
                  class="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all duration-200 bg-gray-50 focus:bg-white"
                  :class="{ 'border-red-300 focus:ring-red-500': errors.roomId }"
                  @input="clearError('roomId')"
                />
              </div>
              <p v-if="errors.roomId" class="text-red-500 text-sm mt-1">{{ errors.roomId }}</p>
            </div>

            <!-- Join Button -->
            <button 
              type="submit"
              :disabled="isConnecting"
              class="w-full bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 disabled:from-gray-400 disabled:to-gray-500 text-white font-semibold py-3 px-6 rounded-xl transition-all duration-200 transform hover:scale-[1.02] disabled:scale-100 shadow-lg hover:shadow-xl disabled:cursor-not-allowed flex items-center justify-center space-x-2"
            >
              <span v-if="!isConnecting">Join Room</span>
              <span v-else class="flex items-center space-x-2">
                <svg class="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Connecting...</span>
              </span>
            </button>
          </form>

          <!-- Quick Actions -->
          <div class="mt-8 pt-6 border-t border-gray-200">
            <div class="flex flex-col sm:flex-row gap-3">
              <button 
                @click="generateRandomRoom"
                class="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-2 px-4 rounded-lg transition-colors duration-200 text-sm"
              >
                🎲 Random Room
              </button>
              <button 
                @click="generateRandomName"
                class="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-2 px-4 rounded-lg transition-colors duration-200 text-sm"
              >
                👤 Random Name
              </button>
            </div>
          </div>

          <!-- Game Rules -->
          <div class="mt-6 p-4 bg-blue-50 rounded-xl border border-blue-200">
            <h3 class="font-semibold text-blue-900 mb-2 flex items-center">
              <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
              </svg>
              How to Play
            </h3>
            <ul class="text-blue-800 text-sm space-y-1">
              <li>• Draw the word when it's your turn</li>
              <li>• Guess what others are drawing</li>
              <li>• Earn points for correct guesses</li>
              <li>• Have fun with friends!</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="text-center mt-6 text-gray-500 text-sm">
        <p>Create memorable moments with friends 🎨</p>
      </div>
    </div>

    <!-- Success Toast -->
    <transition name="toast">
      <div v-if="showSuccessToast" class="fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50">
        <div class="flex items-center space-x-2">
          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/>
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
import { connectWebSocket, getStompClient } from '../ws/stompClient.js';

const username = ref('');
const roomId = ref('');
const isConnecting = ref(false);
const showSuccessToast = ref(false);
const router = useRouter();

// Form validation
const errors = reactive({
  username: '',
  roomId: ''
});

// Random name generator
const randomNames = [
  'ArtistAce', 'DoodleDash', 'SketchStar', 'PaintPro', 'DrawMaster',
  'ColorKing', 'BrushBoss', 'InkHero', 'LineLeader', 'ShadeShark',
  'TintTitan', 'HueHunter', 'PigmentPal', 'CanvasCrafter', 'StrokeSlinger'
];

// Random room generator
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

// Validation functions
const validateForm = () => {
  let isValid = true;
  
  // Reset errors
  errors.username = '';
  errors.roomId = '';
  
  // Validate username
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
  
  // Validate room ID
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

// Join game function
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

      // Navigate to game room
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

// Auto-focus on mount
nextTick(() => {
  const usernameInput = document.getElementById('username');
  if (usernameInput) {
    usernameInput.focus();
  }
});
</script>

<style scoped>
/* Toast animation */
.toast-enter-active, .toast-leave-active {
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

/* Input focus animations */
input:focus {
  transform: translateY(-1px);
}

/* Button hover effects */
button:not(:disabled):hover {
  transform: translateY(-1px);
}

button:not(:disabled):active {
  transform: translateY(0);
}
</style>