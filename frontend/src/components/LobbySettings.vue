<template>
  <div class="lobby">
    <h2>{{ isOwner ? 'Set up your room' : `Waiting for ${ownerName} to start` }}</h2>

    <form class="grid" @submit.prevent>
      <label>Players
        <select v-model.number="form.maxPlayers" class="field" :disabled="!isOwner">
          <option v-for="n in range(2, 12)" :key="n" :value="n" :disabled="n < room.players.length">{{ n }}</option>
        </select>
      </label>
      <label>Rounds
        <select v-model.number="form.rounds" class="field" :disabled="!isOwner">
          <option v-for="n in range(1, 10)" :key="n" :value="n">{{ n }}</option>
        </select>
      </label>
      <label>Draw time
        <select v-model.number="form.drawTime" class="field" :disabled="!isOwner">
          <option v-for="n in DRAW_TIMES" :key="n" :value="n">{{ n }} seconds</option>
        </select>
      </label>
      <label>Word choices
        <select v-model.number="form.wordCount" class="field" :disabled="!isOwner">
          <option v-for="n in range(1, 5)" :key="n" :value="n">{{ n }}</option>
        </select>
      </label>
      <label>Hints
        <select v-model.number="form.hints" class="field" :disabled="!isOwner">
          <option v-for="n in range(0, 5)" :key="n" :value="n">{{ n === 0 ? 'None' : n }}</option>
        </select>
      </label>
      <label class="words">Custom words <span class="aside">separated by commas</span>
        <textarea v-model="customText" class="field" rows="2" :disabled="!isOwner" placeholder="tree house, rubber duck, moonwalk" />
      </label>
      <label class="check">
        <input v-model="form.customOnly" type="checkbox" :disabled="!isOwner" /> Use custom words only
      </label>
    </form>

    <div class="invite">
      <label class="visually-hidden" for="invite">Invite link</label>
      <input id="invite" class="field" readonly :value="link" @focus="$event.target.select()" />
      <button type="button" class="btn" @click="copy">{{ copied ? 'Copied!' : 'Copy invite link' }}</button>
    </div>

    <div v-if="isOwner" class="start">
      <button type="button" class="btn btn-go btn-big" :disabled="room.players.length < 2" @click="send('/app/game/start')">Start game</button>
      <p v-if="room.players.length < 2" class="aside">You need at least one more player. Send them the invite link.</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, watch, ref } from 'vue';
import { store, send } from '../game/store.js';

const DRAW_TIMES = [30, 45, 60, 80, 100, 120, 150, 180, 240];
const range = (a, b) => Array.from({ length: b - a + 1 }, (_, i) => a + i);

const room = computed(() => store.room);
const isOwner = computed(() => room.value.ownerId === store.you);
const ownerName = computed(() => room.value.players.find((p) => p.id === room.value.ownerId)?.name ?? 'the owner');
const link = computed(() => `${location.origin}/?room=${room.value.code}`);

// Only primitives live in `form`: re-assigning identical primitives doesn't re-trigger the save watcher,
// whereas a fresh customWords array from every server echo would loop forever.
const withoutWords = ({ customWords, ...rest }) => rest;
const form = reactive(withoutWords(room.value.settings));
const customText = ref(room.value.settings.customWords.join(', '));
const copied = ref(false);

// Server is the source of truth: follow its settings (the owner's own edits come back the same).
watch(() => room.value.settings, (s) => {
  Object.assign(form, withoutWords(s));
  if (!isOwner.value) customText.value = s.customWords.join(', ');
});

let timer = null;
watch([form, customText], () => {
  if (!isOwner.value) return;
  clearTimeout(timer);
  timer = setTimeout(() => {
    const customWords = customText.value.split(',').map((w) => w.trim()).filter(Boolean);
    send('/app/room/settings', { ...form, customWords });
  }, 300);
});

async function copy() {
  try {
    await navigator.clipboard.writeText(link.value);
  } catch {
    document.getElementById('invite').select();
  }
  copied.value = true;
  setTimeout(() => (copied.value = false), 1600);
}
</script>

<style scoped>
.lobby { display: flex; flex-direction: column; gap: 14px; width: min(560px, 100%); }
h2 { font-size: 2rem; text-align: center; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px 14px; }
label { display: flex; flex-direction: column; gap: 4px; font-weight: 700; font-size: 0.9rem; }
.field { min-height: 40px; padding: 0.35em 0.6em; font-size: 1rem; font-weight: 400; border-width: 2px; }
.words { grid-column: 1 / -1; }
textarea.field { resize: vertical; }
.check { flex-direction: row; align-items: center; gap: 8px; grid-column: 1 / -1; font-weight: 400; }
.check input { width: 20px; height: 20px; accent-color: var(--blue); }
.aside { font-weight: 400; color: var(--ink-soft); font-size: 0.85rem; margin: 0; }
.invite { display: flex; gap: 8px; }
.invite .field { flex: 1; min-width: 0; color: var(--ink-soft); }
.start { display: flex; flex-direction: column; align-items: center; gap: 6px; }
</style>
