// Avatar parts. The server stores plain indexes; anything out of range wraps around.
export const COLORS = [
  '#ff6b6b', '#ffa94d', '#ffd43b', '#a9e34b', '#51cf66', '#20c997',
  '#22b8cf', '#4dabf7', '#5c7cfa', '#845ef7', '#cc5de8', '#f06595',
  '#ff8787', '#e599f7', '#91a7ff', '#c0eb75', '#ffc078', '#96f2d7',
];
export const EYES = 8;
export const MOUTHS = 8;
export const wrap = (v, n) => (((v ?? 0) % n) + n) % n;
