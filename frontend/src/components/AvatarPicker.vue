<template>
  <div class="picker">
    <div class="arrows">
      <button v-for="part in parts" :key="'l' + part.key" type="button" class="arrow" :aria-label="`Previous ${part.label}`" @click="step(part, -1)">‹</button>
    </div>
    <div class="face">
      <Avatar :avatar="modelValue" :size="112" bob label="Your avatar" />
      <button type="button" class="dice btn icon-btn" aria-label="Random avatar" title="Random avatar" @click="$emit('update:modelValue', randomAvatar())">
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="4" fill="none" stroke="currentColor" stroke-width="2.2"/><circle cx="8" cy="8" r="1.6"/><circle cx="16" cy="16" r="1.6"/><circle cx="12" cy="12" r="1.6"/></svg>
      </button>
    </div>
    <div class="arrows">
      <button v-for="part in parts" :key="'r' + part.key" type="button" class="arrow" :aria-label="`Next ${part.label}`" @click="step(part, 1)">›</button>
    </div>
  </div>
</template>

<script setup>
import Avatar from './Avatar.vue';
import { COLORS, EYES, MOUTHS, wrap } from '../game/avatar.js';
import { randomAvatar } from '../game/store.js';

const props = defineProps({ modelValue: { type: Object, required: true } });
const emit = defineEmits(['update:modelValue']);

// Rows line up with the face: eyes on top, body colour in the middle, mouth at the bottom.
const parts = [
  { key: 'eyes', label: 'eyes', n: EYES },
  { key: 'color', label: 'colour', n: COLORS.length },
  { key: 'mouth', label: 'mouth', n: MOUTHS },
];

function step(part, d) {
  emit('update:modelValue', { ...props.modelValue, [part.key]: wrap(props.modelValue[part.key] + d, part.n) });
}
</script>

<style scoped>
.picker { display: flex; align-items: center; justify-content: center; gap: 0.75rem; }
.face { position: relative; padding: 4px; }
.arrows { display: flex; flex-direction: column; gap: 4px; }
.arrow {
  width: 36px; height: 34px;
  border: none; background: none; cursor: pointer;
  font: 400 2.2rem/1 var(--font-hand);
  color: var(--ink);
  border-radius: 8px;
  transition: transform 90ms ease-out;
}
.arrow:hover { color: var(--blue); }
.arrow:active { transform: scale(0.85); }
.dice { position: absolute; right: -14px; bottom: -6px; width: 36px; height: 36px; min-height: 36px; }
</style>
