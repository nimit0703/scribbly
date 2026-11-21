import { describe, it, expect } from 'vitest';
import { floodFill, hexToRgb } from '../render.js';

/** A tiny white image with a black vertical wall at x = 2. */
function image(w = 5, h = 3) {
  const data = new Uint8ClampedArray(w * h * 4).fill(255);
  for (let y = 0; y < h; y++) data.set([0, 0, 0, 255], (y * w + 2) * 4);
  return { data, width: w, height: h };
}
const px = (img, x, y) => Array.from(img.data.slice((y * img.width + x) * 4, (y * img.width + x) * 4 + 3));

describe('floodFill', () => {
  it('fills the connected area and stops at a wall', () => {
    const img = image();
    floodFill(img, 0, 0, [255, 0, 0]);
    expect(px(img, 0, 0)).toEqual([255, 0, 0]);
    expect(px(img, 1, 2)).toEqual([255, 0, 0]);
    expect(px(img, 2, 1)).toEqual([0, 0, 0]); // the wall
    expect(px(img, 4, 1)).toEqual([255, 255, 255]); // other side untouched
  });

  it('is a no-op when filling with the same colour', () => {
    const img = image();
    const before = img.data.slice();
    floodFill(img, 0, 0, [255, 255, 255]);
    expect(img.data).toEqual(before);
  });

  it('clamps out-of-range start points', () => {
    const img = image();
    floodFill(img, 999, -5, [0, 0, 255]);
    expect(px(img, 4, 0)).toEqual([0, 0, 255]);
  });

  it('absorbs near-matching anti-aliased pixels within tolerance', () => {
    const img = image();
    img.data.set([250, 250, 250], (0 * 5 + 1) * 4);
    floodFill(img, 0, 0, [0, 255, 0]);
    expect(px(img, 1, 0)).toEqual([0, 255, 0]);
  });

  it('parses hex colours', () => {
    expect(hexToRgb('#2f5bff')).toEqual([47, 91, 255]);
  });
});
