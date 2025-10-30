<!-- src/components/game/ChatPanel.vue -->
<template>
  <BaseCard header="Chat & Guesses" noPadding>
    <template #header-icon>
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
      </svg>
    </template>

    <div class="flex flex-col h-80">
      <!-- Chat Messages -->
      <div ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-2">
        <div v-if="messages.length === 0" class="text-center text-primary-500 py-8">
          <svg class="w-12 h-12 mx-auto mb-2 text-primary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
          </svg>
          <p class="text-sm">No messages yet</p>
          <p class="text-xs text-primary-400">Start chatting!</p>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="msg.timestamp || msg.content + index"
          class="transition-all duration-300 ease-in-out animate-fadeIn"
        >
          <!-- System Message -->
          <div v-if="msg.username === 'System'" class="bg-primary-100 border border-primary-200 p-2.5 rounded-lg text-center">
            <span class="text-xs font-medium text-primary-700">🎮 {{ msg.content }}</span>
          </div>

          <!-- User Message -->
          <div v-else class="bg-primary-50 rounded-lg p-3 border border-primary-100">
            <div class="flex items-start gap-2">
              <div class="w-6 h-6 rounded-full bg-primary-600 flex items-center justify-center text-white text-xs font-semibold flex-shrink-0">
                {{ msg.username.charAt(0).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-0.5">
                  <span class="font-semibold text-primary-900 text-xs truncate">{{ msg.username }}</span>
                  <span v-if="msg.username === currentUsername" class="text-xs text-accent-600">(You)</span>
                </div>
                <p class="text-sm text-primary-700 break-words">{{ msg.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Chat Input -->
      <div class="p-4 border-t bg-primary-50">
        <div class="flex gap-2">
          <input
            v-model="messageInput"
            @keyup.enter="sendMessage"
            placeholder="Type your guess..."
            class="flex-1 px-3 py-2 bg-white border border-primary-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all text-sm text-primary-900 placeholder-primary-400"
          />
          <BaseButton
            variant="primary"
            size="sm"
            :disabled="!messageInput.trim()"
            @click="sendMessage"
          >
            <template #icon-left>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/>
              </svg>
            </template>
          </BaseButton>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import BaseCard from '../ui/BaseCard.vue';
import BaseButton from '../ui/BaseButton.vue';

const props = defineProps({
  messages: {
    type: Array,
    required: true
  },
  currentUsername: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['send-message']);

const messageInput = ref('');
const chatContainer = ref(null);

const sendMessage = () => {
  if (!messageInput.value.trim()) return;
  emit('send-message', messageInput.value);
  messageInput.value = '';
};

// Auto scroll to bottom when new messages arrive
watch(() => props.messages.length, () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
});
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