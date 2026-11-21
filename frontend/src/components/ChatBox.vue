<template>
  <section class="chat card" aria-label="Chat and guesses">
    <ul ref="list" class="lines" aria-live="polite" @scroll="onScroll">
      <li v-for="(line, i) in store.chat" :key="i" :class="`k-${line.kind}`">
        <template v-if="line.name"><b>{{ line.name }}:</b> {{ line.text }}</template>
        <template v-else>{{ line.text }}</template>
      </li>
    </ul>
    <form class="say" @submit.prevent="submit">
      <label class="visually-hidden" for="guess">{{ placeholder }}</label>
      <input id="guess" ref="input" v-model="text" class="field" maxlength="100" autocomplete="off"
        :placeholder="placeholder" :disabled="store.status !== 'in-room'" />
    </form>
  </section>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue';
import { store, send } from '../game/store.js';

const text = ref('');
const list = ref(null);
const input = ref(null);
let stick = true; // keep following new lines unless the reader scrolled up

const me = computed(() => store.room?.players.find((p) => p.id === store.you));
const drawing = computed(() => store.room?.phase === 'DRAWING');
const placeholder = computed(() => {
  if (!drawing.value) return 'Say hi to the room';
  if (store.room.drawerId === store.you || me.value?.guessed) return 'Chat with people who guessed it';
  return 'Type your guess here';
});

function submit() {
  const t = text.value.trim();
  if (!t) return;
  send('/app/chat', { text: t });
  text.value = '';
}

function onScroll() {
  const el = list.value;
  stick = el.scrollHeight - el.scrollTop - el.clientHeight < 24;
}

watch(() => store.chat.length, async () => {
  if (!stick) return;
  await nextTick();
  list.value.scrollTop = list.value.scrollHeight;
});

// Guessers land straight in the chat box when drawing starts.
watch(drawing, (d) => {
  if (d && store.room.drawerId !== store.you && document.activeElement?.tagName !== 'INPUT') input.value?.focus();
});

defineExpose({ focus: () => input.value?.focus() });
</script>

<style scoped>
.chat { display: flex; flex-direction: column; min-height: 0; overflow: hidden; }
.lines { flex: 1; min-height: 0; overflow-y: auto; margin: 0; padding: 6px 0; list-style: none; font-size: 0.95rem; overscroll-behavior: contain; }
li { padding: 3px 12px; overflow-wrap: anywhere; }
li:nth-child(even) { background: #f6f8fd; }
li.k-system { color: var(--blue); font-weight: 700; }
li.k-success { color: var(--go); font-weight: 700; }
li.k-warn { color: var(--stop); font-weight: 700; }
li.k-close { color: var(--close); font-weight: 700; }
li.k-guessed { color: var(--go); }
.say { padding: 8px; border-top: 2px solid var(--ink); }
.say .field { border-width: 2px; }
</style>
