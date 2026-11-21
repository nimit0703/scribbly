<template>
  <svg :viewBox="def.viewBox" class="doodle" :class="scene" aria-hidden="true">
    <path
      v-for="(p, i) in def.paths"
      :key="scene + i"
      :d="p.d"
      class="draw-on"
      :class="p.cls"
      :style="{ '--len': p.len || 400, '--delay': (p.at || 0) + 's', '--dur': (p.dur || 0.6) + 's' }"
    />
  </svg>
</template>

<script setup>
import { computed } from 'vue';

// Stick-figure scenes, drawn stroke by stroke. `at` is when each line starts drawing (seconds).
const head = (x, y, r = 15) => `M${x} ${y - r} a${r} ${r} 0 1 0 0.1 0`;
const SCENES = {
  // A stick figure draws a house on an easel while a friend scratches their head.
  hero: {
    viewBox: '0 0 420 250',
    paths: [
      { d: 'M14 236 q120 -7 200 -2 t196 0', at: 0, dur: 0.7, len: 420, cls: 'soft' },
      { d: 'M262 236 L292 92 L322 236 M292 92 L292 236', at: 0.25, len: 460 },
      { d: 'M238 68 L348 64 L350 158 L236 161 Z', at: 0.45, len: 420, cls: 'board' },
      { d: head(190, 98), at: 0.8, len: 100 },
      { d: 'M190 113 L187 172 M187 172 L171 228 M187 172 L205 228', at: 1.0, len: 200 },
      { d: 'M189 130 L224 120 M189 130 L166 152', at: 1.25, len: 120 },
      { d: 'M224 120 L246 110', at: 1.4, len: 40, cls: 'pencil' },
      { d: 'M258 142 L258 113 L292 92 L326 113 L326 142 Z M285 142 v-15 h13 v15', at: 1.55, dur: 0.9, len: 300, cls: 'house' },
      { d: 'M262 92 q6 -14 18 -8', at: 2.2, len: 60, cls: 'sun' },
      { d: head(78, 116), at: 2.0, len: 100 },
      { d: 'M78 131 L80 184 M80 184 L64 230 M80 184 L96 230', at: 2.15, len: 200 },
      { d: 'M79 148 L104 166 M79 148 L58 134 L70 108', at: 2.35, len: 140 },
      { d: 'M84 86 a6 6 0 1 0 .1 0 M98 70 a9 9 0 1 0 .1 0 M126 28 c-18 0 -22 14 -16 22 c-10 8 0 24 14 18 c8 10 28 6 30 -6 c14 0 18 -20 4 -26 c0 -12 -24 -16 -32 -8 z', at: 2.6, dur: 0.8, len: 320, cls: 'soft' },
      { d: 'M124 40 q6 -9 13 -3 q5 6 -6 10 v6 M131 62 v1.5', at: 3.1, len: 70, cls: 'question' },
    ],
  },
  // Waiting for players: someone checks their watch and taps a foot.
  waiting: {
    viewBox: '0 0 200 200',
    paths: [
      { d: 'M20 188 q80 -5 160 0', at: 0, len: 170, cls: 'soft' },
      { d: head(100, 52, 16), at: 0.15, len: 110 },
      { d: 'M100 68 L100 130 M100 130 L82 184', at: 0.35, len: 140 },
      { d: 'M100 130 L118 184 L132 182', at: 0.5, len: 80, cls: 'tap' },
      { d: 'M100 86 L76 110 M100 86 L124 92 L112 66', at: 0.6, len: 120 },
      { d: 'M106 64 a5 5 0 1 0 .1 0', at: 0.9, len: 40, cls: 'pencil' },
      { d: 'M140 40 h14 l-14 14 h14 M160 18 h10 l-10 10 h10', at: 1.1, len: 90, cls: 'zz' },
    ],
  },
  // Someone deciding which word to pick.
  choosing: {
    viewBox: '0 0 200 200',
    paths: [
      { d: 'M20 188 q80 -5 160 0', at: 0, len: 170, cls: 'soft' },
      { d: head(80, 78, 16), at: 0.15, len: 110 },
      { d: 'M80 94 L80 148 M80 148 L64 186 M80 148 L96 186', at: 0.35, len: 160 },
      { d: 'M80 110 L60 130 M80 110 L100 96 L92 70', at: 0.55, len: 120 },
      { d: 'M108 52 a5 5 0 1 0 .1 0 M124 38 a8 8 0 1 0 .1 0 M156 6 c-20 0 -26 14 -18 22 c-10 10 4 26 18 18 c10 10 32 4 30 -10 c10 -6 6 -26 -8 -24 c-4 -10 -18 -10 -22 -6 z', at: 0.75, len: 300, cls: 'soft' },
      { d: 'M146 30 h.5 M158 30 h.5 M170 30 h.5', at: 1.2, len: 10, cls: 'dots' },
    ],
  },
  pick: {
    viewBox: '0 0 80 64',
    paths: [
      { d: 'M14 16 L34 10 L40 50 L20 56 Z', at: 0, len: 140 },
      { d: 'M32 12 L52 10 L54 52 L34 54', at: 0.2, len: 140 },
      { d: 'M50 12 L68 18 L60 58 L44 54', at: 0.4, len: 140, cls: 'house' },
    ],
  },
  draw: {
    viewBox: '0 0 80 64',
    paths: [
      { d: 'M8 50 q10 -24 20 -6 t20 -4 t22 -10', at: 0, dur: 0.8, len: 120, cls: 'house' },
      { d: 'M48 12 L66 30 L42 54 L32 56 L34 46 Z M44 16 L62 34', at: 0.5, len: 180 },
    ],
  },
  guess: {
    viewBox: '0 0 80 64',
    paths: [
      { d: 'M10 12 h56 q6 0 6 6 v22 q0 6 -6 6 h-30 l-12 12 v-12 h-14 q-6 0 -6 -6 v-22 q0 -6 6 -6 z', at: 0, len: 260 },
      { d: 'M38 18 v14 M38 39 v1', at: 0.6, len: 40, cls: 'question' },
    ],
  },
};

const props = defineProps({ scene: { type: String, default: 'hero' } });
const def = computed(() => SCENES[props.scene] ?? SCENES.hero);
</script>

<style scoped>
.doodle { display: block; width: 100%; height: auto; overflow: visible; }
path {
  fill: none;
  stroke: var(--ink);
  stroke-width: 3.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.soft { stroke: var(--ink-soft); stroke-width: 2.4; }
.board { fill: #fff; }
.house { stroke: var(--blue); }
.sun { stroke: #f08c00; }
.pencil { stroke: #f2b705; stroke-width: 6; }
.question { stroke: var(--blue); stroke-width: 4; }
.zz { stroke: var(--ink-soft); stroke-width: 2.4; }
.dots { stroke-width: 7; }

/* After the drawing finishes, small loops keep the scene alive. */
.hero .question { animation: draw-on 0.6s ease forwards 3.1s, nod 2.6s ease-in-out 4s infinite; transform-origin: 130px 50px; }
.waiting .tap { transform-origin: 100px 130px; animation: draw-on 0.6s ease forwards 0.5s, tap 0.9s ease-in-out 1.4s infinite; }
.choosing .dots { animation: draw-on 0.3s ease forwards 1.2s, think 1.4s steps(1) 1.6s infinite; }
@keyframes nod { 0%, 100% { transform: rotate(0); } 50% { transform: rotate(-12deg) translateY(-2px); } }
@keyframes tap { 0%, 100% { transform: rotate(0); } 50% { transform: rotate(-9deg); } }
@keyframes think { 0% { opacity: 0.25; } 33% { opacity: 0.6; } 66% { opacity: 1; } }
</style>
