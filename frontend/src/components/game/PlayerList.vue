<!-- src/components/game/PlayerList.vue -->
<template>
  <BaseCard header="Players" noPadding>
    <template #header-icon>
      <i data-feather="users" class="w-4 h-4"></i>
    </template>

    <div class="p-4 max-h-48 overflow-y-auto">
      <div v-if="players.length === 0" class="text-center text-notion-text-muted py-4 ">
        <i data-feather="users" class="w-8 h-8 mx-auto mb-2"></i>
        <p class="text-sm">No players yet</p>
      </div>

      <div v-else class="space-y-2">
        <div v-for="(player, index) in players" :key="player.username"
          class="flex items-center justify-between p-3 rounded-lg transition-all border border-notion-border"
          :class="player.username === currentUsername ? 'bg-notion-border' : 'bg-notion-gray-darker hover:bg-notion-border'">
          <div class="flex items-center gap-3">
            <div :class="getPlayerColorClass(index)"
              class="w-8 h-8 rounded-lg flex items-center justify-center text-notion-text-primary font-semibold text-sm">
              {{ player.username.charAt(0).toUpperCase() }}
            </div>
            <div>
              <div class="font-medium text-notion-text-primary text-sm">
                {{ player.username }}
                <span v-if="player.username === currentUsername" class="text-xs text-notion-accent">(You)</span>
              </div>
            </div>
          </div>
          <div class="text-right">
            <div class="flex items-center gap-1">
              <i data-feather="star" class="w-3.5 h-3.5 text-notion-text-secondary"></i>
              <span class="text-base font-semibold text-notion-text-primary">{{ player.score }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import BaseCard from '../ui/BaseCard.vue';
import { onMounted, nextTick } from 'vue';

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

onMounted(() => {
  nextTick(() => {
    feather.replace();
  });
});

const playerColors = [
  'bg-notion-accent',
  'bg-blue-600',
  'bg-green-600',
  'bg-purple-600',
  'bg-orange-600',
  'bg-pink-600',
  'bg-indigo-600',
  'bg-teal-600'
];

const getPlayerColorClass = (index) => {
  return playerColors[index % playerColors.length];
};
</script>