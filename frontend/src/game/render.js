// Pure canvas rendering for draw ops. The board is always 800x600 logical pixels and
// CSS scales it, so every client sees strokes in the same place whatever its screen size.

export const WIDTH = 800;
export const HEIGHT = 600;

/** Paints one op ({t:'s'|'f', c, s, p}) onto a 2D context. */
export function drawOp(ctx, op) {
  if (op.t === 'f') {
    const img = ctx.getImageData(0, 0, WIDTH, HEIGHT);
    floodFill(img, op.p[0], op.p[1], hexToRgb(op.c));
    ctx.putImageData(img, 0, 0);
    return;
  }
  const p = op.p;
  ctx.strokeStyle = ctx.fillStyle = op.c;
  ctx.lineWidth = op.s;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  if (p.length === 2 || (p.length === 4 && p[0] === p[2] && p[1] === p[3])) {
    ctx.beginPath();
    ctx.arc(p[0], p[1], op.s / 2, 0, Math.PI * 2);
    ctx.fill();
    return;
  }
  ctx.beginPath();
  ctx.moveTo(p[0], p[1]);
  for (let i = 2; i < p.length; i += 2) ctx.lineTo(p[i], p[i + 1]);
  ctx.stroke();
}

/** Clears to white and replays every op. */
export function redraw(ctx, ops) {
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, WIDTH, HEIGHT);
  for (const op of ops) drawOp(ctx, op);
}

export function hexToRgb(hex) {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

/**
 * Scanline flood fill on an ImageData-like {data, width, height}.
 * Tolerance absorbs anti-aliased stroke edges so fills don't leave a white halo.
 */
export function floodFill(img, x, y, [r, g, b], tolerance = 48) {
  const { data, width, height } = img;
  x = Math.max(0, Math.min(width - 1, Math.floor(x)));
  y = Math.max(0, Math.min(height - 1, Math.floor(y)));
  const start = (y * width + x) * 4;
  const tr = data[start], tg = data[start + 1], tb = data[start + 2];
  if (Math.abs(tr - r) + Math.abs(tg - g) + Math.abs(tb - b) === 0) return;

  const seen = new Uint8Array(width * height);
  const matches = (i) => {
    const o = i * 4;
    return !seen[i] && Math.abs(data[o] - tr) + Math.abs(data[o + 1] - tg) + Math.abs(data[o + 2] - tb) <= tolerance;
  };
  const paint = (i) => {
    const o = i * 4;
    data[o] = r; data[o + 1] = g; data[o + 2] = b; data[o + 3] = 255;
    seen[i] = 1;
  };

  const stack = [[x, y]];
  while (stack.length) {
    const [sx, sy] = stack.pop();
    if (!matches(sy * width + sx)) continue;
    let lx = sx;
    while (lx > 0 && matches(sy * width + lx - 1)) lx--;
    let rx = sx;
    while (rx < width - 1 && matches(sy * width + rx + 1)) rx++;
    for (let i = lx; i <= rx; i++) paint(sy * width + i);
    for (const ny of [sy - 1, sy + 1]) {
      if (ny < 0 || ny >= height) continue;
      let inRun = false;
      for (let i = lx; i <= rx; i++) {
        const m = matches(ny * width + i);
        if (m && !inRun) stack.push([i, ny]);
        inRun = m;
      }
    }
  }
}
