<template>
  <section class="players card" aria-label="Players">
    <TransitionGroup tag="ol" name="rank">
      <li v-for="p in ranked" :key="p.id" :class="{ me: p.id === store.you, guessed: p.guessed, drawing: p.id === room.drawerId }">
        <span class="rank">#{{ p.rank }}</span>
        <div class="who">
          <div class="name">
            <span class="text">{{ p.name }}</span>
            <span v-if="p.id === store.you" class="you">(You)</span>
          </div>
          <div class="score">{{ p.score }} points</div>
        </div>
        <div class="pic">
          <Avatar :avatar="p.avatar" :size="44" :label="p.name" />
          <svg v-if="p.id === room.ownerId" class="badge crown" viewBox="0 0 24 16" aria-label="Room owner" role="img"><path d="M2 14 3 3l5 5 4-7 4 7 5-5 1 11z" /></svg>
          <svg v-if="p.id === room.drawerId" class="badge pencil" viewBox="0 0 24 24" aria-label="Drawing" role="img"><path d="M4 20l1.5-5L16 4.5l3.5 3.5L9 18.5z" /></svg>
        </div>
        <button v-if="p.id !== store.you" type="button" class="kick" :title="kickLabel(p)" :aria-label="kickLabel(p)" @click="kick(p)">
          <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18" /></svg>
        </button>
      </li>
    </TransitionGroup>
  </section>
</template>

<script setup>
import { computed } from 'vue';
import Avatar from './Avatar.vue';
import { store, send } from '../game/store.js';

const room = computed(() => store.room);
const isOwner = computed(() => room.value.isPrivate && room.value.ownerId === store.you);

// Highest score first; ties share a rank.
const ranked = computed(() => {
  const list = [...room.value.players].sort((a, b) => b.score - a.score);
  return list.map((p) => ({ ...p, rank: list.findIndex((q) => q.score === p.score) + 1 }));
});

const kickLabel = (p) => (isOwner.value ? `Kick ${p.name}` : `Vote to kick ${p.name}`);
function kick(p) {
  if (isOwner.value) send('/app/kick', { playerId: p.id });
  else send('/app/kick/vote', { playerId: p.id });
}
</script>

<style scoped>
.players { padding: 6px; overflow: auto; }
ol { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
li {
  position: relative;
  display: grid; grid-template-columns: 34px 1fr 48px; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: var(--wobble-sm);
  transition: background-color 200ms;
}
li:nth-child(even) { background: #f3f6fd; }
li.guessed { background: var(--go-wash); }
.rank { font: 1.3rem/1 var(--font-hand); color: var(--ink-soft); }
.who { min-width: 0; }
.name { display: flex; gap: 4px; font-weight: 700; line-height: 1.2; }
.text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.me .text { color: var(--blue); }
.you { font-weight: 400; color: var(--ink-soft); font-size: 0.85rem; }
.score { font-size: 0.85rem; color: var(--ink-soft); font-variant-numeric: tabular-nums; }
.pic { position: relative; justify-self: end; }
.badge { position: absolute; fill: var(--highlight); stroke: var(--ink); stroke-width: 1.8; stroke-linejoin: round; }
.crown { width: 22px; top: -9px; left: 11px; transform: rotate(-8deg); }
.pencil { width: 22px; bottom: -4px; left: -10px; }
.drawing .pic { animation: wiggle 1.2s ease-in-out infinite; }
@keyframes wiggle { 0%, 100% { transform: rotate(-4deg); } 50% { transform: rotate(4deg); } }

.kick {
  position: absolute; top: 2px; right: 2px; width: 24px; height: 24px; display: grid; place-items: center;
  border: 1.5px solid var(--ink); border-radius: 50%; background: #fff; cursor: pointer;
  opacity: 0; transition: opacity 120ms;
}
.kick svg { fill: none; stroke: var(--stop); stroke-width: 3; stroke-linecap: round; }
li:hover .kick, .kick:focus-visible { opacity: 1; }
@media (hover: none) { .kick { opacity: 0.7; } }

.rank-move { transition: transform 450ms cubic-bezier(.2, .8, .2, 1); }
</style>
