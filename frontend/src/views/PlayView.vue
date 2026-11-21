<template>
  <main v-if="store.room" class="game" @keydown="shortcut">
    <GameHeader class="head" :now="now" @leave="leave" />

    <PlayerList class="players" />

    <div class="board">
      <DrawingCanvas :can-draw="canDraw" :tool="tool" :color="color" :size="size">
        <BoardOverlay />
      </DrawingCanvas>
    </div>

    <ChatBox class="chat" />

    <DrawToolbar v-if="canDraw" v-model:color="color" v-model:size="size" v-model:tool="tool" class="tools" />

    <div v-if="store.status === 'lost'" class="lost card" role="alert">
      <p>Lost the connection to the game.</p>
      <button type="button" class="btn btn-primary" @click="rejoin">Rejoin room {{ store.room.code }}</button>
      <button type="button" class="btn" @click="leave">Back to home</button>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import GameHeader from '../components/GameHeader.vue';
import PlayerList from '../components/PlayerList.vue';
import DrawingCanvas from '../components/DrawingCanvas.vue';
import DrawToolbar from '../components/DrawToolbar.vue';
import ChatBox from '../components/ChatBox.vue';
import BoardOverlay from '../components/BoardOverlay.vue';
import { store, leaveRoom, enter, send } from '../game/store.js';

const router = useRouter();
const now = ref(Date.now());
const tool = ref('brush');
const color = ref('#000000');
const size = ref(10);

const canDraw = computed(() => store.room?.phase === 'DRAWING' && store.room.drawerId === store.you);

let clock = null;
onMounted(() => (clock = setInterval(() => (now.value = Date.now()), 250)));
onBeforeUnmount(() => clearInterval(clock));

// New turn: fresh black brush.
watch(() => store.room?.drawerId, () => {
  tool.value = 'brush';
  color.value = '#000000';
  size.value = 10;
});

// Kicked or left: back to the home page.
watch(() => store.room, (r) => {
  if (!r) router.replace({ name: 'home' });
});

function leave() {
  leaveRoom();
}

function rejoin() {
  const code = store.room.code;
  enter('join', code);
}

/** B / F / E switch tools, Ctrl+Z undoes, while drawing and not typing. */
function shortcut(e) {
  if (!canDraw.value || e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
  const k = e.key.toLowerCase();
  if ((e.ctrlKey || e.metaKey) && k === 'z') {
    e.preventDefault();
    send('/app/draw/undo');
  } else if (k === 'b') tool.value = 'brush';
  else if (k === 'f') tool.value = 'fill';
  else if (k === 'e') tool.value = 'eraser';
}
</script>

<style scoped>
.game {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr) 290px;
  grid-template-areas:
    'head    head  head'
    'players board chat'
    '.       tools .';
  gap: 14px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 14px 16px 24px;
}
.head { grid-area: head; }
.players { grid-area: players; align-self: start; max-height: 100%; }
.board { grid-area: board; min-width: 0; }
.chat { grid-area: chat; position: relative; }
.tools { grid-area: tools; }

/* The chat must not stretch the row: the board decides the height, the chat fills it. */
@media (min-width: 961px) {
  .chat { contain: size; }
}

@media (max-width: 960px) {
  .game {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    grid-template-areas:
      'head    head'
      'board   board'
      'tools   tools'
      'players chat';
    gap: 10px;
    padding: 8px 8px 16px;
  }
  .players, .chat { height: 320px; }
}

@media (max-width: 520px) {
  .game {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas: 'head' 'board' 'tools' 'chat' 'players';
  }
  .players { height: auto; max-height: 300px; }
}

.lost {
  position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 10;
  display: flex; flex-wrap: wrap; align-items: center; gap: 10px; padding: 12px 16px;
}
.lost p { margin: 0; font-weight: 700; }
</style>
