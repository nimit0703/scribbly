<!-- src/components/game/ChatPanel.vue -->
<template>
  <BaseCard header="Chat & Guesses" noPadding>
    <template #header-icon>
      <i data-feather="message-square" class="w-4 h-4"></i>
    </template>

    <div class="flex flex-col h-80">
      <!-- Chat Messages -->
      <div ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-2">
        <div v-if="messages.length === 0" class="text-center text-notion-text-muted py-8">
          <i data-feather="message-square" class="w-8 h-8 mx-auto mb-2"></i>
          <p class="text-sm">No messages yet</p>
          <p class="text-xs">Start chatting!</p>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="msg.timestamp || msg.content + index"
          class="transition-all duration-300 ease-in-out animate-fadeIn"
        >
          <!-- System Message -->
          <div v-if="msg.username === 'System'" class="bg-notion-gray-dark border border-notion-border p-2.5 rounded-lg text-center">
            <span class="text-xs font-medium text-notion-text-secondary">🎮 {{ msg.content }}</span>
          </div>

          <!-- User Message -->
          <div v-else class="bg-notion-gray-darker rounded-lg p-3 border border-notion-border">
            <div class="flex items-start gap-2">
              <div class="w-6 h-6 rounded-lg bg-notion-accent flex items-center justify-center text-notion-text-primary text-xs font-semibold flex-shrink-0">
                {{ msg.username.charAt(0).toUpperCase() }}
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-0.5">
                  <span class="font-semibold text-notion-text-primary text-xs truncate">{{ msg.username }}</span>
                  <span v-if="msg.username === currentUsername" class="text-xs text-notion-accent">(You)</span>
                </div>
                <p class="text-sm text-notion-text-primary break-words">{{ msg.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Chat Input -->
      <div class="p-4 border-t border-notion-border bg-notion-gray-dark">
        <div class="flex gap-2">
          <input
            v-model="messageInput"
            @keyup.enter="sendMessage"
            placeholder="Type your guess..."
            class="flex-1 px-3 py-2 bg-notion-gray-darker border border-notion-border rounded-lg focus:outline-none focus:border-notion-border-hover transition-all text-sm text-notion-text-primary placeholder-notion-text-muted"
          />
          <BaseButton
            variant="accent"
            size="sm"
            :disabled="!messageInput.trim()"
            @click="sendMessage"
          >
            <i data-feather="send" class="w-4 h-4"></i>
          </BaseButton>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue';
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

onMounted(() => {
  nextTick(() => {
    feather.replace();
  });
});

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