<!-- src/components/game/PlayerList.vue -->
<template>
  <BaseCard header="Players" noPadding>
    <template #header-icon>
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
      </svg>
    </template>
    
    <div class="p-4 max-h-48 overflow-y-auto">
      <div v-if="players.length === 0" class="text-center text-primary-500 py-4">
        <svg class="w-12 h-12 mx-auto mb-2 text-primary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
        </svg>
        <p class="text-sm">No players yet</p>
      </div>
      
      <div v-else class="space-y-2">
        <div 
          v-for="(player, index) in players" 
          :key="player.username"
          class="flex items-center justify-between p-3 rounded-lg transition-all"
          :class="player.username === currentUsername ? 'bg-accent-50 border-2 border-accent-200' : 'bg-primary-50 hover:bg-primary-100'"
        >
          <div class="flex items-center gap-3">
            <div 
              :class="getPlayerColorClass(index)"
              class="w-9 h-9 rounded-full flex items-center justify-center text-white font-semibold text-sm"
            >
              {{ player.username.charAt(0).toUpperCase() }}
            </div>
            <div>
              <div class="font-medium text-primary-900 text-sm">
                {{ player.username }}
                <span v-if="player.username === currentUsername" class="text-xs text-accent-600">(You)</span>
              </div>
            </div>
          </div>
          <div class="text-right">
            <div class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"/>
              </svg>
              <span class="text-base font-bold text-primary-900">{{ player.score }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import BaseCard from '../ui/BaseCard.vue';

defineProps({
  players: {
    type: Array,
    required: true
  },
  currentUsername: {
    type: String,
    required: true
  }
});

const playerColors = [
  'bg-primary-600',
  'bg-accent-600',
  'bg-blue-600',
  'bg-green-600',
  'bg-orange-600',
  'bg-pink-600',
  'bg-indigo-600',
  'bg-teal-600'
];

const getPlayerColorClass = (index) => {
  return playerColors[index % playerColors.length];
};
</script>