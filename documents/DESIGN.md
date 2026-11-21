# Visual design

**Concept: a sheet of graph paper.** The page is blue graph paper. Every UI piece is a paper cut-out outlined in ballpoint ink, with a slightly uneven, hand-drawn border and a hard ink shadow. The secret word gets a highlighter. Stick figures draw themselves on screen. The game is about drawing badly with friends, so the interface looks drawn too, but stays calm enough that the drawing on the board is always the loudest thing on screen.

Scribbly follows the classic draw-and-guess game loop, but has its own look. It doesn't copy any existing game's logo, art or palette.

## Tokens (`frontend/src/style.css`)

| Token | Value | Used for |
|---|---|---|
| `--paper` | `#f4f7fd` | Page, sheets over the board |
| `--grid` | `#d9e3f5` | Graph-paper lines |
| `--ink` | `#1e2a4f` | Text, outlines, shadows |
| `--ink-soft` | `#56628a` | Secondary text |
| `--blue` | `#2f5bff` | Links, focus ring, system chat, "you" |
| `--highlight` | `#ffe14d` | The word, revealed hint letters, selected tool, 1st place |
| `--go` | `#18a058` | Play/Start, correct guesses, guessed rows |
| `--stop` | `#e5484d` | Timer under 10 s, warnings, kick |
| `--close` | `#c77800` | "is close!" |

Shape: `--wobble-sm` (uneven border radii that read as hand-drawn) and `--shadow` / `--shadow-sm` (hard 4–5 px ink offset).

## Type

- **Gochi Hand** for display: the logo, headings, buttons, the word, the clock. It reads as handwriting without becoming illegible at button sizes.
- **Atkinson Hyperlegible** for everything people read under time pressure: chat, names, scores, settings. It was designed for maximum character distinction, which matters when you're scanning guesses fast.

## Motion

- **Doodles** (`Doodle.vue`) draw themselves once with `stroke-dashoffset`, then keep one small loop going (the question mark nods, a foot taps, thinking dots pulse).
- **Avatars** blink on random offsets so a full room doesn't blink in unison; the drawer's avatar wiggles.
- **Sheets** over the board fade and scale in and out along the same path (260 ms). A sheet on its way out ignores the pointer.
- **Buttons** respond on press (`:active` sinks into its shadow), not on release.
- The clock shakes and turns red in the last 10 s, with a tick sound.
- Everything collapses to near-instant under `prefers-reduced-motion`; the doodles show fully drawn.

## Layout

```
Desktop (> 960 px)                      Tablet (≤ 960 px)        Phone (≤ 520 px)
+-------------------------------------+ +-------------------+     +-----------+
| clock  round   _ _ a _ _   🔊 Leave | | header            |     | header    |
+--------+------------------+---------+ +-------------------+     +-----------+
| players| board 4:3        | chat    | | board             |     | board     |
|        |                  |         | +-------------------+     | tools     |
|        |                  |         | | tools             |     | chat      |
+--------+------------------+---------+ +---------+---------+     | players   |
         | tools (drawer)   |           | players | chat    |     +-----------+
         +------------------+           +---------+---------+
```

The board is always 4:3; on desktop the chat column takes its height from the board (`contain: size`). In a private lobby on phones the board grows to fit the settings.

## Accessibility

- Visible focus ring everywhere (`:focus-visible`, 3 px blue).
- Tools, colours and sizes are radio groups with labels; the clock is a `timer`; chat is a polite live region.
- Touch targets are at least 40–44 px. The canvas uses `touch-action: none` so drawing doesn't scroll the page.
- Colour is never the only signal: guessed players also get the word shown, close guesses say "is close!", and the drawer has a pencil badge.
