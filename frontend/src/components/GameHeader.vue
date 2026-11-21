<template>
  <header class="bar card">
    <div class="clock" :class="{ urgent }" :aria-label="`${timeLeft} seconds left`" role="timer">
      <svg viewBox="0 0 60 60" aria-hidden="true">
        <circle cx="30" cy="33" r="24" class="face" />
        <path d="M16 10 l-6 6 M44 10 l6 6 M26 6 h8" class="ears" />
        <circle cx="30" cy="33" r="24" class="sweep" pathLength="100" :style="{ strokeDashoffset: 100 - progress * 100 }" />
      </svg>
      <span class="secs">{{ showTimer ? timeLeft : '' }}</span>
    </div>

    <div class="round">
      <template v-if="inGame">Round {{ room.round }} of {{ room.settings.rounds }}</template>
      <template v-else>Room {{ room.code }}</template>
    </div>

    <div class="word" aria-live="polite">
      <template v-if="room.phase === 'DRAWING' && store.word">
        <span class="label">{{ isDrawer ? 'Draw this' : 'You got it' }}</span>
        <span class="known">{{ store.word }}</span>
      </template>
      <template v-else-if="room.phase === 'DRAWING' && room.hint">
        <span class="label">Guess this</span>
        <span class="hint-row">
          <span class="slots" :aria-label="`${letters} letters`">
            <span v-for="(ch, i) in room.hint" :key="i" :class="ch === ' ' ? 'gap' : ch === '_' ? 'slot' : 'slot shown'">{{ ch === '_' ? '' : ch }}</span>
          </span>
          <sup class="count" aria-hidden="true">{{ lengths }}</sup>
        </span>
      </template>
      <span v-else class="label big">{{ waitingText }}</span>
    </div>

    <div class="actions">
      <button type="button" class="btn icon-btn" :aria-label="store.muted ? 'Turn sound on' : 'Mute sound'" :title="store.muted ? 'Sound off' : 'Sound on'" @click="setMuted(!store.muted)">
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <path d="M4 9h4l5-4v14l-5-4H4z" />
          <path v-if="!store.muted" d="M16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12" />
          <path v-else d="M17 9l5 6M22 9l-5 6" />
        </svg>
      </button>
      <button type="button" class="btn leave" @click="$emit('leave')">Leave</button>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { store, setMuted } from '../game/store.js';
import { play } from '../game/sound.js';

const props = defineProps({ now: { type: Number, required: true } });
defineEmits(['leave']);

const room = computed(() => store.room);
const isDrawer = computed(() => room.value.drawerId === store.you);
const inGame = computed(() => ['CHOOSING', 'DRAWING', 'TURN_END'].includes(room.value.phase));
const showTimer = computed(() => room.value.phase !== 'LOBBY');
const timeLeft = computed(() => Math.max(0, Math.ceil((store.deadline - props.now) / 1000)));
// The time left when a phase starts is its full length; the sweep empties from there.
const total = ref(1);
watch(() => [room.value.phase, room.value.drawerId, room.value.round], () => {
  total.value = Math.max(1, room.value.timeLeft);
}, { immediate: true });
const progress = computed(() => (showTimer.value ? Math.min(1, timeLeft.value / total.value) : 1));
const urgent = computed(() => room.value.phase === 'DRAWING' && timeLeft.value <= 10);
const letters = computed(() => (room.value.hint || '').replace(/[^_\p{L}\p{N}]/gu, '').length);
const lengths = computed(() => (room.value.hint || '').split(' ').map((w) => w.length).join(' '));
const waitingText = computed(() => {
  const r = room.value;
  const drawer = r.players.find((p) => p.id === r.drawerId);
  switch (r.phase) {
    case 'CHOOSING': return isDrawer.value ? 'Pick a word!' : `${drawer?.name ?? 'Someone'} is choosing…`;
    case 'TURN_END': return r.turnResult?.word ? `It was “${r.turnResult.word}”` : 'Next turn coming up';
    case 'GAME_OVER': return 'Game over!';
    default: return r.isPrivate ? 'Waiting in the lobby' : 'Waiting for players';
  }
});

// Ticking in the last 10 seconds, once per second.
watch(timeLeft, (t, prev) => {
  if (urgent.value && t < prev && t > 0 && !store.muted) play('tick');
});
</script>

<style scoped>
.bar {
  display: grid; grid-template-columns: auto auto 1fr auto; align-items: center; gap: 16px;
  padding: 8px 12px; box-shadow: var(--shadow-sm);
}
.clock { position: relative; width: 60px; height: 60px; flex: none; }
.clock svg { width: 100%; height: 100%; overflow: visible; }
.face { fill: #fff; stroke: var(--ink); stroke-width: 3; }
.ears { fill: none; stroke: var(--ink); stroke-width: 3; stroke-linecap: round; }
.sweep {
  fill: none; stroke: var(--highlight); stroke-width: 7; opacity: 0.9;
  stroke-dasharray: 100; transform: rotate(-90deg) scale(0.78); transform-origin: 30px 33px;
  transition: stroke-dashoffset 1s linear;
}
.secs { position: absolute; inset: 6px 0 0; display: grid; place-items: center; font: 1.5rem/1 var(--font-hand); font-variant-numeric: tabular-nums; }
.urgent .sweep { stroke: #ffb3b5; }
.urgent .secs { color: var(--stop); }
.urgent { animation: shake 0.5s ease-in-out infinite; }
@keyframes shake { 0%, 100% { transform: rotate(0); } 25% { transform: rotate(-6deg); } 75% { transform: rotate(6deg); } }

.round { font: 1.35rem/1 var(--font-hand); white-space: nowrap; }

.word { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 0; text-align: center; }
.label { font-size: 0.8rem; color: var(--ink-soft); }
.label.big { font: 1.6rem/1.1 var(--font-hand); color: var(--ink); }
.known {
  font: 2rem/1 var(--font-hand); letter-spacing: 0.04em; padding: 0 10px;
  background: linear-gradient(transparent 18%, var(--highlight) 18%, var(--highlight) 88%, transparent 88%);
}
.hint-row { display: flex; align-items: flex-start; }
.slots { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px; }
.slot {
  width: 22px; height: 30px; display: grid; place-items: end center;
  border-bottom: 3px solid var(--ink); font: 1.8rem/1 var(--font-hand); text-transform: lowercase;
}
.slot.shown { background: linear-gradient(transparent 30%, var(--highlight) 30%); animation: pop 400ms cubic-bezier(.3, 1.6, .5, 1); }
.gap { width: 14px; }
.count { font: 1rem/1 var(--font-hand); color: var(--ink-soft); margin-left: 4px; }
@keyframes pop { from { transform: scale(0.4); } }

.actions { display: flex; gap: 8px; }
.icon-btn svg { fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
.leave { font-size: 1.15rem; min-height: 40px; }

@media (max-width: 720px) {
  .bar { grid-template-columns: auto 1fr auto; gap: 8px; }
  .round { grid-column: 2; grid-row: 1; justify-self: center; font-size: 1.1rem; }
  .word { grid-column: 1 / -1; grid-row: 2; }
  .actions { grid-column: 3; grid-row: 1; }
}
</style>
