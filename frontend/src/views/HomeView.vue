<template>
  <main class="home">
    <header class="masthead">
      <h1 class="logo" aria-label="Scribbly">
        <span v-for="(ch, i) in 'scribbly'" :key="i" :style="{ '--r': tilt[i] }" aria-hidden="true">{{ ch }}</span>
        <svg class="underline" viewBox="0 0 300 24" aria-hidden="true">
          <path class="draw-on" style="--len: 340; --delay: .2s; --dur: .8s" d="M6 14 C60 4 110 20 160 11 S250 6 294 13" />
        </svg>
      </h1>
      <p class="tagline">Draw it badly. Guess it fast.</p>
    </header>

    <section class="stage">
      <div class="art"><Doodle scene="hero" /></div>

      <form class="card entry" @submit.prevent="go(inviteCode ? 'join' : 'quick')">
        <AvatarPicker v-model="store.profile.avatar" />

        <label class="visually-hidden" for="name">Your name</label>
        <input id="name" v-model="store.profile.name" class="field name" maxlength="20" autocomplete="nickname"
          placeholder="Enter your name" />

        <p v-if="store.kicked" class="notice" role="alert">You were kicked from the room.</p>
        <p v-else-if="store.error" class="notice" role="alert">{{ store.error }}</p>

        <template v-if="inviteCode">
          <button class="btn btn-go btn-big" :disabled="busy">{{ busy ? 'Joining…' : `Join room ${inviteCode}` }}</button>
          <button type="button" class="btn" :disabled="busy" @click="forgetInvite">Play somewhere else</button>
        </template>
        <template v-else>
          <button class="btn btn-go btn-big" :disabled="busy">{{ busy ? 'Finding a room…' : 'Play!' }}</button>
          <button type="button" class="btn" :disabled="busy" @click="go('create')">Create private room</button>
          <div class="code-row">
            <label class="visually-hidden" for="code">Room code</label>
            <input id="code" v-model="code" class="field" maxlength="12" placeholder="Have a code?"
              autocapitalize="characters" @keydown.enter.prevent="code && go('join', code)" />
            <button type="button" class="btn" :disabled="busy || !code.trim()" @click="go('join', code)">Join</button>
          </div>
        </template>
      </form>
    </section>

    <section class="how" aria-labelledby="how-title">
      <h2 id="how-title">How to play</h2>
      <ol>
        <li><Doodle scene="pick" class="step-art" /><strong>Pick a word.</strong> When it's your turn, choose one of three words.</li>
        <li><Doodle scene="draw" class="step-art" /><strong>Draw it.</strong> No letters, no numbers. Just your best scribbles.</li>
        <li><Doodle scene="guess" class="step-art" /><strong>Guess fast.</strong> Type guesses in the chat. Quicker guesses earn more points.</li>
      </ol>
    </section>
  </main>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AvatarPicker from '../components/AvatarPicker.vue';
import Doodle from '../components/Doodle.vue';
import { store, enter } from '../game/store.js';

const route = useRoute();
const router = useRouter();
const code = ref('');
const inviteCode = computed(() => (route.query.room || '').toString().toUpperCase().slice(0, 12));
const busy = computed(() => store.status === 'connecting');
const tilt = ['-4deg', '3deg', '-2deg', '5deg', '-3deg', '2deg', '-5deg', '4deg'];

async function go(mode, roomCode = inviteCode.value) {
  store.kicked = false;
  await enter(mode, roomCode?.trim());
}

function forgetInvite() {
  router.replace({ query: {} });
}

watch(() => store.status, (s) => {
  if (s === 'in-room') router.push({ name: 'play' });
});
</script>

<style scoped>
.home { max-width: 1080px; margin: 0 auto; padding: 32px 16px 64px; }

.masthead { text-align: center; margin-bottom: 20px; }
.logo { position: relative; display: inline-block; font-size: clamp(4rem, 12vw, 7.5rem); line-height: 0.9; letter-spacing: -0.02em; }
.logo span { display: inline-block; transform: rotate(var(--r)); transition: transform 220ms cubic-bezier(.3,1.6,.5,1); }
.logo:hover span { transform: rotate(0) translateY(-3px); }
.underline { position: absolute; left: 4%; right: 4%; bottom: -14px; width: 92%; }
.underline path { fill: none; stroke: var(--blue); stroke-width: 6; stroke-linecap: round; }
.tagline { margin: 22px 0 0; font: 1.5rem/1.2 var(--font-hand); color: var(--ink-soft); }

.stage { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(300px, 380px); gap: 40px; align-items: center; }
.art { padding: 12px; }

.entry { display: flex; flex-direction: column; gap: 12px; padding: 22px; }
.name { text-align: center; font-size: 1.2rem; font-weight: 700; }
.code-row { display: flex; gap: 8px; }
.code-row .field { text-transform: uppercase; }
.notice { margin: 0; padding: 8px 12px; border-radius: var(--wobble-sm); background: #ffe3e3; color: #a51d24; font-weight: 700; }

.how { margin-top: 56px; }
.how h2 { font-size: 2.4rem; text-align: center; margin-bottom: 20px; }
.how ol { list-style: none; counter-reset: step; display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; padding: 0; margin: 0; }
.how li { counter-increment: step; position: relative; padding: 18px 18px 18px 22px; background: var(--card); border: 2px solid var(--ink); border-radius: var(--wobble-sm); max-width: 40ch; }
.how li::before { content: counter(step); position: absolute; top: -18px; left: -12px; width: 38px; height: 38px; display: grid; place-items: center; background: var(--highlight); border: 2px solid var(--ink); border-radius: 50%; font: 1.5rem/1 var(--font-hand); }
.step-art { width: 80px; margin-bottom: 6px; }
.how strong { display: block; font: 1.5rem/1.1 var(--font-hand); font-weight: 400; margin-bottom: 4px; }

@media (max-width: 820px) {
  .stage { grid-template-columns: 1fr; gap: 12px; }
  .art { order: 2; max-width: 420px; margin: 0 auto; }
  .how ol { grid-template-columns: 1fr; gap: 28px; }
}
</style>
