// Tiny synthesized sound effects (no audio files). Each cue is a list of [frequency Hz, start s, length s].
const CUES = {
  join: [[523, 0, 0.08], [784, 0.08, 0.12]],
  leave: [[440, 0, 0.08], [330, 0.08, 0.14]],
  turn: [[660, 0, 0.1], [880, 0.1, 0.1], [660, 0.2, 0.12]],
  start: [[392, 0, 0.06], [523, 0.06, 0.12]],
  guess: [[523, 0, 0.07], [659, 0.07, 0.07], [784, 0.14, 0.07], [1047, 0.21, 0.18]],
  end: [[523, 0, 0.12], [392, 0.12, 0.12], [330, 0.24, 0.2]],
  win: [[523, 0, 0.1], [659, 0.1, 0.1], [784, 0.2, 0.1], [1047, 0.3, 0.1], [784, 0.4, 0.08], [1047, 0.48, 0.3]],
  tick: [[1200, 0, 0.03]],
};

let ctx = null;

export function play(name) {
  const cue = CUES[name];
  if (!cue) return;
  try {
    ctx ??= new AudioContext();
    const t0 = ctx.currentTime;
    for (const [freq, start, len] of cue) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.value = freq;
      gain.gain.setValueAtTime(0.0001, t0 + start);
      gain.gain.exponentialRampToValueAtTime(0.12, t0 + start + 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, t0 + start + len);
      osc.connect(gain).connect(ctx.destination);
      osc.start(t0 + start);
      osc.stop(t0 + start + len + 0.02);
    }
  } catch {
    /* audio blocked or unsupported: the game works silently */
  }
}
