<template>
  <div class="toolbar card" role="toolbar" aria-label="Drawing tools">
    <div class="swatches" role="radiogroup" aria-label="Colour">
      <button
        v-for="c in PALETTE" :key="c" type="button" class="swatch" role="radio"
        :aria-checked="c === color" :aria-label="c" :style="{ background: c }"
        :class="{ on: c === color }"
        @click="$emit('update:color', c); tool === 'eraser' && $emit('update:tool', 'brush')"
      />
    </div>

    <div class="group" role="radiogroup" aria-label="Brush size">
      <button v-for="s in SIZES" :key="s" type="button" class="tool" role="radio" :aria-checked="s === size"
        :aria-label="`Size ${s}`" :class="{ on: s === size }" @click="$emit('update:size', s)">
        <span class="dot" :style="{ width: dot(s), height: dot(s), background: tool === 'eraser' ? '#fff' : color }" />
      </button>
    </div>

    <div class="group" role="radiogroup" aria-label="Tool">
      <button v-for="t in TOOLS" :key="t.id" type="button" class="tool" role="radio" :aria-checked="t.id === tool"
        :class="{ on: t.id === tool }" :title="`${t.label} (${t.key})`" :aria-label="t.label" @click="$emit('update:tool', t.id)">
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path :d="t.icon" /></svg>
      </button>
    </div>

    <div class="group">
      <button type="button" class="tool" title="Undo (Ctrl+Z)" aria-label="Undo" @click="send('/app/draw/undo')">
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path d="M9 14 4 9l5-5M4 9h10.5a5.5 5.5 0 0 1 0 11H11" /></svg>
      </button>
      <button type="button" class="tool danger" title="Clear the board" aria-label="Clear the board" @click="send('/app/draw/clear')">
        <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3" /></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { send } from '../game/store.js';

defineProps({ color: String, size: Number, tool: String });
defineEmits(['update:color', 'update:size', 'update:tool']);

const PALETTE = [
  '#ffffff', '#c5cbd8', '#e5484d', '#ff8a3d', '#ffd43b', '#40c057', '#22b8cf', '#2f5bff', '#7950f2', '#f06595', '#b5763c', '#ffd8a8',
  '#000000', '#5c6475', '#a61e24', '#c2410c', '#c9a100', '#1b7a3a', '#0b7285', '#1d2f9e', '#4c2a9e', '#a61e4d', '#6b3e1f', '#e8a87c',
];
const SIZES = [4, 10, 20, 36];
const TOOLS = [
  { id: 'brush', label: 'Brush', key: 'B', icon: 'M4 20c3 0 5-2 5-4.5S11 12 13 12l7-8-2-2-8 7c0 2-1.5 4-4 4S4 15 4 20Z' },
  { id: 'fill', label: 'Fill', key: 'F', icon: 'M5 12l7-7 7 7-7 7zM19 15s2 2.5 2 4a2 2 0 0 1-4 0c0-1.5 2-4 2-4ZM5 12h14' },
  { id: 'eraser', label: 'Eraser', key: 'E', icon: 'M8 20h12M5 15l9-9 5 5-9 9H8z M10 10l5 5' },
];
const dot = (s) => `${Math.max(4, Math.min(26, s * 0.7))}px`;
</script>

<style scoped>
.toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 14px; padding: 10px 12px; box-shadow: var(--shadow-sm); }
.swatches { display: grid; grid-template-columns: repeat(12, 24px); grid-auto-rows: 24px; border: 2px solid var(--ink); border-radius: 4px; overflow: hidden; }
.swatch { border: 0; padding: 0; cursor: pointer; position: relative; }
.swatch.on::after { content: ''; position: absolute; inset: 4px; border: 2.5px solid #fff; outline: 2px solid var(--ink); border-radius: 50%; }
.swatch:focus-visible { outline-offset: -3px; z-index: 1; }
.group { display: flex; gap: 6px; }
.tool {
  width: 44px; height: 44px; display: grid; place-items: center;
  background: #fff; border: 2px solid var(--ink); border-radius: var(--wobble-sm);
  cursor: pointer; transition: transform 90ms ease-out, background-color 120ms;
}
.tool:active { transform: scale(0.92); }
.tool.on { background: var(--highlight); }
.tool svg { fill: none; stroke: var(--ink); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.tool.danger:hover { background: #ffe3e3; }
.dot { display: block; border-radius: 50%; border: 1.5px solid var(--ink); }

@media (max-width: 560px) {
  .swatches { grid-template-columns: repeat(12, minmax(0, 1fr)); width: 100%; }
}
</style>
