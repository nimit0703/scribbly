<template>
  <div class="board" :class="{ live: canDraw }" @pointerleave="cursor.show = false">
    <canvas
      ref="canvas"
      :width="WIDTH"
      :height="HEIGHT"
      :aria-label="canDraw ? 'Drawing board. Draw here.' : 'Drawing board'"
      role="img"
      @pointerdown="down"
      @pointermove="move"
      @pointerup="up"
      @pointercancel="up"
    />
    <div v-if="canDraw && cursor.show" class="cursor" :style="cursorStyle" aria-hidden="true" />
    <slot />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { WIDTH, HEIGHT, drawOp, redraw } from '../game/render.js';
import { store, onCanvas, send, addLocalOps } from '../game/store.js';

const props = defineProps({
  canDraw: Boolean,
  tool: { type: String, default: 'brush' }, // brush | fill | eraser
  color: { type: String, default: '#000000' },
  size: { type: Number, default: 10 },
});

const FLUSH_MS = 40;
const canvas = ref(null);
let ctx = null;
let off = null;
let group = Date.now() % 1_000_000_000; // ids only need to grow within a turn
let stroke = null;
let flushTimer = null;
const cursor = reactive({ show: false, x: 0, y: 0, scale: 1 });

const cursorStyle = computed(() => {
  const d = Math.max(4, props.size * cursor.scale);
  return { width: `${d}px`, height: `${d}px`, transform: `translate(${cursor.x - d / 2}px, ${cursor.y - d / 2}px)` };
});

onMounted(() => {
  ctx = canvas.value.getContext('2d', { willReadFrequently: true });
  redraw(ctx, store.ops);
  off = onCanvas((kind, ops) => {
    if (kind === 'sync') redraw(ctx, ops);
    else ops.forEach((op) => drawOp(ctx, op));
  });
});
onBeforeUnmount(() => {
  off?.();
  clearTimeout(flushTimer);
});

function toBoard(e) {
  const r = canvas.value.getBoundingClientRect();
  cursor.x = e.clientX - r.left;
  cursor.y = e.clientY - r.top;
  cursor.scale = r.width / WIDTH;
  return [Math.round((cursor.x * WIDTH) / r.width), Math.round((cursor.y * HEIGHT) / r.height)];
}

function emit(op) {
  addLocalOps([op]);
  send('/app/draw', { ops: [op] });
}

function down(e) {
  if (!props.canDraw || e.button > 0) return;
  canvas.value.setPointerCapture(e.pointerId);
  const [x, y] = toBoard(e);
  if (props.tool === 'fill') {
    const op = { t: 'f', g: ++group, c: props.color, s: 1, p: [x, y] };
    drawOp(ctx, op);
    emit(op);
    return;
  }
  const c = props.tool === 'eraser' ? '#ffffff' : props.color;
  stroke = { g: ++group, c, s: props.size, last: [x, y], pending: [x, y], sent: false };
  drawOp(ctx, { t: 's', c, s: props.size, p: [x, y] });
}

function move(e) {
  cursor.show = true;
  if (!stroke) {
    if (props.canDraw) toBoard(e);
    return;
  }
  // Coalesced events recover the points the browser merged between frames: smoother fast strokes.
  for (const ev of e.getCoalescedEvents?.() ?? [e]) {
    const [x, y] = toBoard(ev);
    const [lx, ly] = stroke.last;
    if (x === lx && y === ly) continue;
    drawOp(ctx, { t: 's', c: stroke.c, s: stroke.s, p: [lx, ly, x, y] });
    stroke.pending.push(x, y);
    stroke.last = [x, y];
  }
  flushTimer ??= setTimeout(flush, FLUSH_MS);
}

function up() {
  flush();
  stroke = null;
}

/** Sends the points drawn since the last flush. Each chunk starts at the previous chunk's last point so they join up. */
function flush() {
  clearTimeout(flushTimer);
  flushTimer = null;
  if (!stroke || (stroke.sent && stroke.pending.length <= 2)) return;
  emit({ t: 's', g: stroke.g, c: stroke.c, s: stroke.s, p: stroke.pending });
  stroke.sent = true;
  stroke.pending = [...stroke.last];
}
</script>

<style scoped>
.board {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #fff;
  border: 2.5px solid var(--ink);
  border-radius: 6px 3px 7px 4px / 4px 7px 3px 6px;
  box-shadow: var(--shadow);
  overflow: hidden;
}
canvas { display: block; width: 100%; height: 100%; touch-action: none; }
.live canvas { cursor: none; }
@media (max-width: 720px) {
  .board:has(.overlay.lobby) { aspect-ratio: auto; }
  .board:has(.overlay.lobby) canvas { position: absolute; inset: 0; }
}
.cursor {
  position: absolute; top: 0; left: 0;
  border: 1.5px solid var(--ink);
  outline: 1.5px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  pointer-events: none;
}
</style>
