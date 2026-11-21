import { reactive } from 'vue';
import { connect, disconnect } from '../ws/stompClient.js';
import { play } from './sound.js';

const MAX_CHAT = 150;

function load(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key)) ?? fallback;
  } catch {
    return fallback;
  }
}
function save(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch { /* private mode: settings just won't stick */ }
}

export function freshState() {
  return {
    status: 'idle', // idle | connecting | in-room | lost
    error: '',
    kicked: false,
    you: null,
    room: null, // RoomView from the server (see documents/PROTOCOL.md)
    deadline: 0, // local epoch ms when the current phase ends
    word: null, // the secret word, only if you're drawing or already guessed
    words: [], // word choices, only while you're choosing
    ops: [], // full drawing history of this turn
    chat: [],
  };
}

export const store = reactive({
  ...freshState(),
  profile: load('scribbly.profile', { name: '', avatar: randomAvatar() }),
  muted: load('scribbly.muted', false),
});

/** Canvas listens here: ('ops', newOps) to paint on top, ('sync', allOps) to repaint. */
const canvasListeners = new Set();
export function onCanvas(fn) {
  canvasListeners.add(fn);
  return () => canvasListeners.delete(fn);
}
const emitCanvas = (kind, ops) => canvasListeners.forEach((fn) => fn(kind, ops));

export function randomAvatar() {
  const r = (n) => Math.floor(Math.random() * n);
  return { color: r(18), eyes: r(8), mouth: r(8) };
}

/**
 * Applies one server message to state. Pure apart from mutating `s`; returns the sound to play (if any)
 * and canvas effects, so it can be unit tested without a socket.
 */
export function reduce(s, msg, now = Date.now()) {
  const fx = { sound: null, canvas: null };
  switch (msg.type) {
    case 'welcome':
      // Our own "joined" line was broadcast before we subscribed to the room, so add it locally.
      if (!s.room) s.chat.push({ kind: 'success', text: `You joined room ${msg.state.code}` });
      s.you = msg.you;
      s.word = msg.word ?? null;
      s.words = msg.words ?? [];
      s.ops = msg.ops ?? [];
      fx.canvas = ['sync', s.ops];
      setRoom(s, msg.state, now);
      break;
    case 'state': {
      const prev = s.room;
      setRoom(s, msg.state, now);
      const r = msg.state;
      if (prev && prev.phase !== r.phase) {
        if (r.phase === 'CHOOSING') s.word = null;
        if (r.phase === 'DRAWING') fx.sound = 'start';
        if (r.phase === 'TURN_END') fx.sound = 'end';
        if (r.phase === 'GAME_OVER') fx.sound = 'win';
      }
      if (r.phase !== 'CHOOSING') s.words = [];
      if (r.phase === 'TURN_END' && r.turnResult?.word) s.word = r.turnResult.word;
      break;
    }
    case 'chat':
      s.chat.push(msg.line);
      if (s.chat.length > MAX_CHAT) s.chat.splice(0, s.chat.length - MAX_CHAT);
      if (msg.line.kind === 'success' && msg.line.text.endsWith('guessed the word!')) fx.sound = 'guess';
      else if (msg.line.kind === 'success' && msg.line.text.endsWith('joined the room!')) fx.sound = 'join';
      else if (msg.line.kind === 'warn' && msg.line.text.endsWith('left the room.')) fx.sound = 'leave';
      break;
    case 'draw':
      // The drawer already painted these locally while drawing.
      if (s.room?.drawerId !== s.you) {
        s.ops.push(...msg.ops);
        fx.canvas = ['ops', msg.ops];
      }
      break;
    case 'sync':
      s.ops = msg.ops;
      fx.canvas = ['sync', s.ops];
      break;
    case 'words':
      s.words = msg.words;
      fx.sound = 'turn';
      break;
    case 'word':
      s.word = msg.word;
      break;
    case 'kicked':
      s.kicked = true;
      break;
    case 'error':
      s.error = msg.message;
      break;
  }
  return fx;
}

function setRoom(s, room, now) {
  s.room = room;
  s.deadline = now + room.timeLeft * 1000;
}

// ------------------------------------------------------------------ actions

let client = null;
let roomSub = null;
let meSub = null;

function handle(frame) {
  const fx = reduce(store, JSON.parse(frame.body));
  if (store.room && store.status === 'connecting') store.status = 'in-room';
  if (store.room && !roomSub) subscribeRoom(store.room.code);
  if (store.kicked) leaveRoom();
  if (fx.canvas) emitCanvas(...fx.canvas);
  if (fx.sound && !store.muted) play(fx.sound);
}

function subscribeRoom(code) {
  roomSub = client.subscribe(`/topic/room/${code}`, handle);
  // Anything broadcast between our join and this subscription would be lost: ask for a fresh snapshot.
  send('/app/room/sync');
}

/** mode: 'quick' | 'create' | 'join' */
export async function enter(mode, code) {
  const { name, avatar } = store.profile;
  save('scribbly.profile', store.profile);
  Object.assign(store, freshState(), { status: 'connecting' });
  try {
    client = await connect(() => {
      store.status = 'lost';
      roomSub = null;
    });
  } catch (e) {
    store.status = 'idle';
    store.error = e.message;
    return;
  }
  roomSub?.unsubscribe();
  roomSub = null;
  meSub?.unsubscribe();
  meSub = client.subscribe('/user/queue/me', (frame) => {
    handle(frame);
    if (store.error && !store.room) store.status = 'idle';
  });
  send(`/app/room/${mode}`, { name, avatar, code });
}

export function leaveRoom() {
  send('/app/room/leave');
  roomSub?.unsubscribe();
  roomSub = null;
  const kicked = store.kicked;
  Object.assign(store, freshState(), { kicked });
  disconnect();
  client = null;
  meSub = null;
}

export function send(destination, body = {}) {
  if (client?.connected) client.publish({ destination, body: JSON.stringify(body) });
}

export function setMuted(v) {
  store.muted = v;
  save('scribbly.muted', v);
}

/** Local echo for the drawer so strokes appear with zero latency. */
export function addLocalOps(ops) {
  store.ops.push(...ops);
}
