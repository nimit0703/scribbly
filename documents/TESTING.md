# Testing

Three layers: fast backend tests, fast frontend tests, and a browser game that plays itself.

| Layer | Command | Count | Runtime |
|---|---|---|---|
| Backend (JUnit 5, Mockito, AssertJ) | `cd backend/game && ./mvnw test` | 35 tests | ~15 s |
| Frontend (Vitest) | `cd frontend && npm test` | 12 tests | < 1 s |
| End-to-end (real Chrome, playwright-core) | `cd frontend && npm run e2e` | 2 scenarios | ~1 min |

## Backend

| File | What it proves |
|---|---|
| `service/RulesTest` | Hint masking, hint cap and schedule, guess normalisation, close guesses (Levenshtein = 1, ≥ 4 letters), scoring formula, settings clamping, draw-op validation, name/chat cleaning, kick threshold. |
| `service/GameEngineTest` | Whole games through `GameEngine` with a fake `Clock` and a mocked `GameBroadcaster`: owner-only start and settings, unknown or full rooms, quick-play matching and auto start, a full turn with hints and scores, close-guess whisper, guessed-players private chat, the drawer can't type the word, choose and draw timeouts, 2 rounds to podium to lobby, drawer-only drawing, undo by gesture, clear, drawer/guesser leaving, room deletion, vote kick, owner kick, one like per turn, switching rooms. |
| `config/WebSocketConfigTest` | The inbound filter: clients can't send to `/topic/**` or subscribe to another session's queue. |
| `GameApplicationTests` | Boots the real server on a random port. Two STOMP-over-SockJS clients create a room, join by code, start, choose, draw, guess and disconnect. Also checks that a frame forged straight to `/topic/room/…` never reaches anyone. |

Time never passes by sleeping: `GameEngineTest.FakeClock` is advanced by hand and `engine.tick(room, now)` is called directly.

## Frontend

| File | What it proves |
|---|---|
| `src/game/__tests__/render.test.js` | Flood fill stops at walls, is a no-op on the same colour, clamps start points, absorbs anti-aliased edges; hex parsing. |
| `src/game/__tests__/store.test.js` | `reduce()` for every message type: welcome, phase changes clearing the word, the turn result revealing it, the drawer not repainting its own echo, sync replacing the drawing, chat cap and sounds, errors and kicks. |

## End-to-end (`frontend/e2e/play.mjs`)

Needs both servers running. It drives the installed Chrome, so no browser download (set `E2E_CHANNEL=msedge` to use Edge, `HEADED=1` to watch).

1. **Private game.** Alice (desktop) creates a room, sets 1 round / 45 s / custom words. Bob (desktop) and Cara (phone, 390×844) join through the invite link, and the settings sync to them. Alice starts. Each of the 3 turns: the drawer picks a word, draws a house with the mouse (strokes, colour change, flood fill), one guesser sends a wrong guess, a close guess (checked for the whisper), the word, then chats privately; the other guesser gets it; the result sheet appears. Then the podium, back to the lobby, and Cara leaves.
2. **Public game.** Dan and Eve press **Play!** and land in the same room, where the game starts by itself. Fay joins mid-game; Dan and Eve vote-kick her; Fay is sent home with a notice.

Screenshots are written to `documents/screenshots/` on every run.

## Bugs the browser run caught

These passed the unit tests and only showed up when playing for real:

- The "pick a word" sheet kept catching pointer events while fading out, so a drawer who started drawing right away lost their first stroke (`.sheet-leave-active { pointer-events: none }`).
- A chat line of kind `chat` got the CSS class `chat`, which collided with the chat panel's own flex layout (classes are now `k-<kind>`).
- The podium was clipped on phones (container query on the board's height).
- The lobby settings were squeezed into the 4:3 board on phones (the board grows in the lobby).
