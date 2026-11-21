<template>
  <Transition name="sheet">
    <div v-if="view" :key="view + (room.drawerId || '')" class="overlay" :class="view">
      <!-- Lobby -->
      <LobbySettings v-if="view === 'lobby'" />

      <div v-else-if="view === 'waiting'" class="center">
        <Doodle scene="waiting" class="art" />
        <h2>Waiting for players</h2>
        <p>The game starts as soon as someone else joins.</p>
      </div>

      <!-- Choosing -->
      <div v-else-if="view === 'choose'" class="center">
        <h2>Pick a word to draw</h2>
        <div class="choices">
          <button v-for="(w, i) in store.words" :key="w" type="button" class="btn btn-big choice"
            :style="{ '--r': ['-2deg', '1.5deg', '-1deg', '2deg', '-1.5deg'][i] }"
            @click="send('/app/game/choose', { index: i })">{{ w }}</button>
        </div>
      </div>

      <div v-else-if="view === 'choosing'" class="center">
        <Doodle scene="choosing" class="art" />
        <h2>{{ drawer?.name }} is choosing a word</h2>
      </div>

      <!-- Turn result -->
      <div v-else-if="view === 'result'" class="center">
        <p class="reason">{{ result.reason }}</p>
        <h2 v-if="result.word">The word was <span class="mark">{{ result.word }}</span></h2>
        <ul class="gains">
          <li v-for="p in gains" :key="p.id">
            <span class="n">{{ p.name }}</span>
            <span :class="p.gain ? 'plus' : 'zero'">+{{ p.gain }}</span>
          </li>
        </ul>
      </div>

      <!-- Podium -->
      <div v-else-if="view === 'podium'" class="center">
        <h2>{{ podium[0]?.name }} wins!</h2>
        <div class="podium">
          <div v-for="(p, i) in podiumOrder" :key="p.id" class="step" :class="`place-${p.place}`" :style="{ '--i': i }">
            <svg v-if="p.place === 1" class="crown" viewBox="0 0 24 16" aria-hidden="true"><path d="M2 14 3 3l5 5 4-7 4 7 5-5 1 11z" /></svg>
            <Avatar :avatar="p.avatar" :size="p.place === 1 ? 84 : 64" bob :label="p.name" />
            <div class="pname">{{ p.name }}</div>
            <div class="block"><span>{{ p.place }}</span><small>{{ p.score }} pts</small></div>
          </div>
        </div>
        <ol v-if="rest.length" class="rest" start="4">
          <li v-for="p in rest" :key="p.id">{{ p.name }}, {{ p.score }} pts</li>
        </ol>
      </div>
    </div>
  </Transition>

  <!-- Guessers can react to the drawing -->
  <div v-if="canRate" class="rate">
    <button type="button" class="btn icon-btn" aria-label="Like this drawing" title="Like" :disabled="rated" @click="rate(true)">
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path d="M7 11v9H4v-9zM7 11l4-8c1.5 0 2.5 1 2.2 2.6L12.6 9H19a2 2 0 0 1 2 2.3l-1.2 7A2 2 0 0 1 17.8 20H7" /></svg>
    </button>
    <button type="button" class="btn icon-btn" aria-label="Dislike this drawing" title="Dislike" :disabled="rated" @click="rate(false)">
      <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true" style="transform: rotate(180deg)"><path d="M7 11v9H4v-9zM7 11l4-8c1.5 0 2.5 1 2.2 2.6L12.6 9H19a2 2 0 0 1 2 2.3l-1.2 7A2 2 0 0 1 17.8 20H7" /></svg>
    </button>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import Avatar from './Avatar.vue';
import Doodle from './Doodle.vue';
import LobbySettings from './LobbySettings.vue';
import { store, send } from '../game/store.js';

const room = computed(() => store.room);
const isDrawer = computed(() => room.value.drawerId === store.you);
const drawer = computed(() => room.value.players.find((p) => p.id === room.value.drawerId));
const result = computed(() => room.value.turnResult ?? {});

const view = computed(() => {
  switch (room.value.phase) {
    case 'LOBBY': return room.value.isPrivate ? 'lobby' : 'waiting';
    case 'CHOOSING': return isDrawer.value && store.words.length ? 'choose' : 'choosing';
    case 'TURN_END': return 'result';
    case 'GAME_OVER': return 'podium';
    default: return null;
  }
});

const gains = computed(() =>
  room.value.players.map((p) => ({ ...p, gain: result.value.gains?.[p.id] ?? 0 })).sort((a, b) => b.gain - a.gain));

const ranked = computed(() => {
  const list = [...room.value.players].sort((a, b) => b.score - a.score);
  return list.map((p) => ({ ...p, place: list.findIndex((q) => q.score === p.score) + 1 }));
});
const podium = computed(() => ranked.value.slice(0, 3));
// 2nd, 1st, 3rd: the winner stands in the middle.
const podiumOrder = computed(() => [podium.value[1], podium.value[0], podium.value[2]].filter(Boolean));
const rest = computed(() => ranked.value.slice(3));

const rated = ref(false);
const canRate = computed(() => room.value.phase === 'DRAWING' && !isDrawer.value);
watch(() => room.value.drawerId, () => (rated.value = false));
function rate(like) {
  rated.value = true;
  send('/app/rate', { like });
}
</script>

<style scoped>
.overlay {
  position: absolute; inset: 0; z-index: 2;
  display: flex; align-items: center; justify-content: center;
  padding: 20px; overflow: auto;
  background: rgba(244, 247, 253, 0.94);
  container-type: size;
}
.overlay.lobby { align-items: flex-start; }
/* The turn result lets the drawing show through faintly; the lobby and podium are a clean page. */
.overlay.lobby, .overlay.waiting, .overlay.podium { background: var(--paper); }
.center { display: flex; flex-direction: column; align-items: center; gap: 12px; text-align: center; margin: auto; }
.center h2 { font-size: clamp(1.6rem, 3.6vw, 2.4rem); }
.center p { margin: 0; color: var(--ink-soft); }
.art { width: min(180px, 32vw); }

.choices { display: flex; flex-wrap: wrap; justify-content: center; gap: 14px; }
.choice { transform: rotate(var(--r)); background: #fff; }
.choice:hover { background: var(--highlight); }
.choice:active { transform: rotate(var(--r)) translate(2px, 3px); }

.reason { font-weight: 700; }
.mark { padding: 0 8px; background: linear-gradient(transparent 20%, var(--highlight) 20%, var(--highlight) 90%, transparent 90%); }
.gains { list-style: none; margin: 0; padding: 0; min-width: 220px; display: flex; flex-direction: column; gap: 4px; }
.gains li { display: flex; justify-content: space-between; gap: 24px; font-size: 1.05rem; }
.n { font-weight: 700; }
.plus { color: var(--go); font-weight: 700; }
.zero { color: var(--ink-soft); }

.podium { display: flex; align-items: flex-end; justify-content: center; gap: 10px; }
.step { display: flex; flex-direction: column; align-items: center; gap: 4px; animation: rise 600ms cubic-bezier(.2, .9, .3, 1.2) both; animation-delay: calc(var(--i) * 150ms); }
.place-1 { order: 0; }
.pname { font-weight: 700; max-width: 12ch; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.block {
  width: 96px; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; padding-top: 6px;
  background: #fff; border: 2.5px solid var(--ink); border-bottom: none; border-radius: 6px 6px 0 0;
  font: 2rem/1 var(--font-hand);
}
.block small { font: 0.8rem/1.4 var(--font-body); color: var(--ink-soft); }
.place-1 .block { height: 110px; background: var(--highlight); }
.place-2 .block { height: 80px; }
.place-3 .block { height: 60px; }
.crown { width: 40px; fill: var(--highlight); stroke: var(--ink); stroke-width: 1.6; stroke-linejoin: round; margin-bottom: -8px; transform: rotate(-8deg); }
.rest { margin: 0; color: var(--ink-soft); }
@keyframes rise { from { opacity: 0; transform: translateY(40px); } }

.rate { position: absolute; top: 10px; right: 10px; z-index: 1; display: flex; gap: 6px; }
.rate svg { fill: none; stroke: currentColor; stroke-width: 2; stroke-linejoin: round; }

/* Small boards (phones): everything on the sheet shrinks to fit the 4:3 area. */
@container (max-height: 380px) {
  .center { gap: 6px; }
  .center h2 { font-size: 1.4rem; }
  .art { width: 96px; }
  .step :deep(.avatar) { width: 40px; height: 40px; }
  .block { width: 72px; font-size: 1.4rem; }
  .place-1 .block { height: 64px; }
  .place-2 .block { height: 48px; }
  .place-3 .block { height: 36px; }
  .crown { width: 26px; margin-bottom: -4px; }
  .pname { font-size: 0.85rem; }
  .gains li { font-size: 0.9rem; }
  .btn-big { font-size: 1.3rem; min-height: 44px; }
}

/* Phones: nothing is drawn in the lobby, so the settings sheet takes the space it needs. */
@media (max-width: 720px) {
  .overlay.lobby { position: relative; inset: auto; container-type: normal; padding: 16px 12px; }
}

/* Overlays materialise: blur and scale together, symmetric on the way out. */
.sheet-enter-active, .sheet-leave-active { transition: opacity 260ms ease, transform 260ms cubic-bezier(.2, .8, .2, 1); }
.sheet-enter-from, .sheet-leave-to { opacity: 0; transform: scale(0.97); }
/* A sheet on its way out must not swallow the drawer's first stroke. */
.sheet-leave-active { pointer-events: none; }
</style>
