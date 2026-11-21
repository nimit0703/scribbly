# Game rules

These are the rules the server enforces. Code references point at where each rule lives.

## Rooms

- **Public rooms** (`Play!`): you join the fullest public room with a free seat (max 8). The game starts by itself as soon as there are 2 players, and anyone who joins later plays from the current round on. After the podium a new game starts automatically.
- **Private rooms** (`Create private room`): you're the owner (crown). You pick the settings and press **Start game** when at least 2 players are in. Share the invite link (`/?room=CODE`) or the 6-letter code. After the podium the room goes back to the lobby with the same people.
- If the owner leaves, the next player in join order becomes owner.

| Setting | Range | Default |
|---|---|---|
| Players | 2–12 (never below the current head count) | 8 |
| Rounds | 1–10 | 3 |
| Draw time | 15–240 s | 80 s |
| Word choices | 1–5 | 3 |
| Hints | 0–5 | 2 |
| Custom words | ≤ 500, comma-separated | none |
| Custom words only | on/off (needs at least *word choices* custom words) | off |

## Turns and rounds

1. **Choosing** (15 s): the drawer sees the word choices. If they don't pick in time, one is picked at random.
2. **Drawing** (draw time): everyone else guesses in the chat.
3. **Turn end** (6 s): the word is revealed with everyone's points for that turn.

A turn ends early when every guesser has the word, or when the drawer leaves. A **round** is over when everyone has drawn once. After the last round comes the **podium** (10 s). If fewer than 2 players remain at any point mid-game, the game goes straight to the podium.

## Hints

With `hints = n`, the server reveals up to `n` letters during drawing, but never more than half the word's letters (`Hints.hintCount`). Reveals are evenly spaced: with 2 hints in 90 s, letters appear at 30 s and 60 s. Spaces, hyphens and punctuation are always visible, so `ice cream` shows as `___ _____` with lengths `3 5`.

## Guessing

- A guess is **correct** when it matches the word ignoring case, spaces and punctuation (`icecream`, `Ice-Cream` and `ice cream` all match `ice cream`). It must be the whole message.
- A guess is **close** when it's exactly one letter off (added, missing or wrong) and the word has at least 4 letters. Only the guesser is told.
- Once you've guessed, your messages only reach the drawer and other players who guessed, so you can't spoil the word.
- The drawer can chat with players who guessed, but can't send a message that contains the word.

## Scoring (`Scoring.java`)

**Guesser:** `50 + round(350 × timeLeft / drawTime) + order bonus`, where the order bonus is 100 for the first correct guess, 50 for the second and 25 for the third. The most you can get is 500 (first guess, instantly); the least is 50.

**Drawer:** half of every correct guesser's points, so a drawing that everyone gets quickly pays the most. If nobody guesses, the drawer gets nothing.

Example, 80 s draw time, 3 guessers:

| Guess | Time left | Guesser gets | Drawer gets |
|---|---|---|---|
| 1st | 60 s | 50 + 263 + 100 = **413** | 206 |
| 2nd | 40 s | 50 + 175 + 50 = **275** | 137 |
| 3rd | 10 s | 50 + 44 + 25 = **119** | 59 |

Drawer total for the turn: 402.

## Being nice

- **Like / dislike**: each guesser can rate the drawing once per turn. It shows in the chat.
- **Vote kick**: any player can vote to kick another. It takes `max(2, ceil((players − 1) / 2))` votes, so one person can never kick alone and a 2-player room can't kick at all. The owner of a private room can kick directly.
- A kicked player is sent back to the home page.
