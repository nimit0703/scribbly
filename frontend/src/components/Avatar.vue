<template>
  <svg class="avatar" :class="{ bob }" viewBox="0 0 100 100" :width="size" :height="size" role="img" :aria-label="label">
    <path class="body" :fill="fill"
      d="M50 7 C75 6 93 24 92 50 C93 77 73 94 49 92 C24 93 6 75 8 50 C7 25 25 8 50 7 Z" />
    <path class="shine" d="M27 26 q8 -9 18 -10" />
    <g class="eyes" :style="{ animationDelay: blinkDelay }">
      <template v-if="eyes === 0">
        <circle cx="36" cy="43" r="5.5" class="ink-fill" /><circle cx="64" cy="43" r="5.5" class="ink-fill" />
      </template>
      <template v-else-if="eyes === 1">
        <circle cx="35" cy="42" r="10" class="white" /><circle cx="65" cy="42" r="10" class="white" />
        <circle cx="38" cy="44" r="4.5" class="ink-fill" /><circle cx="68" cy="44" r="4.5" class="ink-fill" />
      </template>
      <path v-else-if="eyes === 2" class="ink" d="M27 46 q8 -11 17 0 M56 46 q8 -11 17 0" />
      <template v-else-if="eyes === 3">
        <path class="ink" d="M27 41 q8 7 17 0 M56 41 q8 7 17 0" />
        <path class="ink thin" d="M29 36 h13 M58 36 h13" />
      </template>
      <template v-else-if="eyes === 4">
        <circle cx="36" cy="43" r="5.5" class="ink-fill" />
        <path class="ink" d="M56 44 q8 -8 16 0" />
      </template>
      <template v-else-if="eyes === 5">
        <circle cx="37" cy="45" r="5" class="ink-fill" /><circle cx="63" cy="45" r="5" class="ink-fill" />
        <path class="ink" d="M25 32 l17 7 M75 32 l-17 7" />
      </template>
      <template v-else-if="eyes === 6">
        <circle cx="35" cy="43" r="11" class="glass" /><circle cx="65" cy="43" r="11" class="glass" />
        <path class="ink thin" d="M46 42 q4 -3 8 0 M24 41 l-8 -3 M76 41 l8 -3" />
        <circle cx="35" cy="44" r="3.5" class="ink-fill" /><circle cx="65" cy="44" r="3.5" class="ink-fill" />
      </template>
      <path v-else class="ink" d="M30 37 l11 11 M41 37 l-11 11 M59 37 l11 11 M70 37 l-11 11" />
    </g>
    <g class="mouth">
      <path v-if="mouth === 0" class="ink" d="M36 64 q14 14 28 0" />
      <path v-else-if="mouth === 1" class="ink ink-fill-dark" d="M34 62 q16 22 32 0 z" />
      <path v-else-if="mouth === 2" class="ink" d="M38 69 h24" />
      <ellipse v-else-if="mouth === 3" cx="50" cy="69" rx="6" ry="7" class="ink ink-fill-dark" />
      <template v-else-if="mouth === 4">
        <path class="tongue" d="M45 69 q5 13 10 0 z" />
        <path class="ink" d="M36 65 q14 10 28 0" />
      </template>
      <template v-else-if="mouth === 5">
        <path class="ink white" d="M34 63 h32 q-2 13 -16 13 q-14 0 -16 -13 z" />
        <path class="ink thin" d="M35 68 h30" />
      </template>
      <path v-else-if="mouth === 6" class="ink" d="M38 69 q14 4 24 -7" />
      <path v-else class="ink" d="M33 68 q4.25 -5 8.5 0 t8.5 0 t8.5 0 t8.5 0" />
    </g>
  </svg>
</template>

<script setup>
import { computed } from 'vue';
import { COLORS, EYES, MOUTHS, wrap as mod } from '../game/avatar.js';


const props = defineProps({
  avatar: { type: Object, default: () => ({ color: 0, eyes: 0, mouth: 0 }) },
  size: { type: [Number, String], default: 48 },
  bob: Boolean,
  label: { type: String, default: 'Avatar' },
});

const fill = computed(() => COLORS[mod(props.avatar.color, COLORS.length)]);
const eyes = computed(() => mod(props.avatar.eyes, EYES));
const mouth = computed(() => mod(props.avatar.mouth, MOUTHS));
// Each avatar blinks on its own schedule so a room full of them doesn't blink in unison.
const blinkDelay = `${(Math.random() * 5).toFixed(2)}s`;
</script>

<style scoped>
.avatar { display: block; overflow: visible; flex: none; }
.body { stroke: var(--ink); stroke-width: 4; }
.shine { fill: none; stroke: #fff; stroke-width: 4; stroke-linecap: round; opacity: 0.55; }
.ink { fill: none; stroke: var(--ink); stroke-width: 4.5; stroke-linecap: round; stroke-linejoin: round; }
.ink.thin { stroke-width: 3; }
.ink-fill { fill: var(--ink); }
.ink-fill-dark { fill: #4a1d2e; }
.white { fill: #fff; stroke: var(--ink); stroke-width: 3.5; }
.glass { fill: rgba(255, 255, 255, 0.55); stroke: var(--ink); stroke-width: 3.5; }
.tongue { fill: #ff8fab; stroke: var(--ink); stroke-width: 3; }

.eyes { transform-origin: 50px 43px; animation: blink 6s infinite; }
@keyframes blink {
  0%, 94%, 100% { transform: scaleY(1); }
  96% { transform: scaleY(0.1); }
}
.bob { animation: bob 2.4s ease-in-out infinite; }
@keyframes bob {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-4px) rotate(2deg); }
}
</style>
