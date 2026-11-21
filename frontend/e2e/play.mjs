// End-to-end: three real browsers play a full private game against running servers.
//   backend:  cd backend/game && ./mvnw spring-boot:run
//   frontend: cd frontend && npm run dev
//   then:     npm run e2e            (E2E_URL defaults to http://localhost:5173)
// Uses the locally installed Chrome through playwright-core (no browser download).
// Screenshots land in documents/screenshots/.
import { chromium } from 'playwright-core';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const BASE = process.env.E2E_URL || 'http://localhost:5173';
const SHOTS = fileURLToPath(new URL('../../documents/screenshots/', import.meta.url));
mkdirSync(SHOTS, { recursive: true });

const log = (...a) => console.log('•', ...a);
const shot = (page, name) => page.screenshot({ path: `${SHOTS}${name}.png` });

const browser = await chromium.launch({ channel: process.env.E2E_CHANNEL || 'chrome', headless: !process.env.HEADED });

async function player(name, viewport) {
  const ctx = await browser.newContext({ viewport, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => { throw new Error(`${name}: ${e.message}`); });
  page.name = name;
  return page;
}

/** The drawer is whoever is offered word choices. */
async function findDrawer(pages) {
  for (let i = 0; i < 60; i++) {
    for (const p of pages) if (await p.getByText('Pick a word to draw').isVisible()) return p;
    await pages[0].waitForTimeout(250);
  }
  throw new Error('nobody was asked to pick a word');
}

/** Draws a little house with the mouse, then fills it. */
async function drawHouse(page) {
  const box = await page.locator('canvas').boundingBox();
  const at = (x, y) => [box.x + (x / 800) * box.width, box.y + (y / 600) * box.height];
  const path = async (pts) => {
    await page.mouse.move(...at(...pts[0]));
    await page.mouse.down();
    for (const p of pts.slice(1)) await page.mouse.move(...at(...p), { steps: 8 });
    await page.mouse.up();
  };
  await path([[250, 450], [250, 280], [550, 280], [550, 450], [250, 450]]);
  await path([[230, 290], [400, 150], [570, 290]]);
  await page.getByRole('radio', { name: '#2f5bff' }).click();
  await path([[370, 450], [370, 360], [430, 360], [430, 450]]);
  await page.getByRole('radio', { name: 'Fill' }).click();
  await page.getByRole('radio', { name: '#ffd43b' }).click();
  await page.mouse.click(...at(300, 320));
  await page.getByRole('radio', { name: 'Brush' }).click();
  await page.getByRole('radio', { name: '#e5484d' }).click();
  await path([[620, 140], [660, 120], [700, 150], [690, 190], [640, 190], [620, 140]]);
}

async function say(page, text) {
  const box = page.locator('#guess');
  await box.fill(text);
  await box.press('Enter');
}

try {
  const alice = await player('Alice', { width: 1440, height: 900 });
  const bob = await player('Bob', { width: 1280, height: 820 });
  const cara = await player('Cara', { width: 390, height: 844 });
  const all = [alice, bob, cara];

  // --- home and room creation
  await alice.goto(BASE);
  await alice.getByText('How to play').waitFor();
  await alice.waitForTimeout(3800); // let the doodle finish drawing itself
  await shot(alice, '01-home');
  await alice.getByPlaceholder('Enter your name').fill('Alice');
  await alice.getByRole('button', { name: 'Create private room' }).click();
  await alice.getByText('Set up your room').waitFor();
  const invite = await alice.locator('#invite').inputValue();
  log('room created', invite);

  await alice.getByLabel('Rounds').selectOption('1');
  await alice.getByLabel('Draw time').selectOption('45');
  await alice.getByRole('textbox', { name: /Custom words/ }).fill('rubber duck, moonwalk');

  // --- friends join through the invite link
  for (const p of [bob, cara]) {
    await p.goto(invite);
    await p.getByPlaceholder('Enter your name').fill(p.name);
    await p.getByRole('button', { name: /Join room/ }).click();
    await p.getByText(/Waiting for Alice to start/).waitFor();
  }
  await alice.waitForTimeout(500);
  await shot(alice, '02-lobby');
  await shot(cara, '03-lobby-phone');
  if (await bob.getByLabel('Rounds').inputValue() !== '1') throw new Error('settings did not sync to Bob');

  await alice.getByRole('button', { name: 'Start game' }).click();

  // --- one round: everyone draws once
  for (let turn = 1; turn <= 3; turn++) {
    const drawer = await findDrawer(all);
    const guessers = all.filter((p) => p !== drawer);
    if (turn === 1) {
      await guessers[0].waitForTimeout(1500); // let the doodle draw itself
      await shot(guessers[0], '04-choosing');
      await shot(drawer, '04-choose-word');
    }
    await drawer.locator('.choice').first().click();
    const word = (await drawer.locator('.known').innerText()).trim();
    log(`turn ${turn}: ${drawer.name} draws "${word}"`);

    // Like a person, start drawing once the tools are on screen.
    await drawer.getByRole('toolbar', { name: 'Drawing tools' }).waitFor();
    await drawHouse(drawer);
    await say(guessers[0], 'is it a banana?');
    const close = word.length >= 4 ? word.slice(0, -1) + (word.at(-1) === 'x' ? 'y' : 'x') : null;
    if (close) await say(guessers[0], close);
    await guessers[0].waitForTimeout(400);
    if (turn === 1) {
      await shot(guessers[0], '05-guessing');
      await shot(drawer, '06-drawing');
      if (close && !(await guessers[0].getByText(/is close!/).isVisible())) throw new Error('close guess not flagged');
    }
    await say(guessers[0], word);
    await guessers[0].locator('.known').waitFor(); // guesser now sees the word
    await say(guessers[0], 'that was a nice one');
    await say(guessers[1], word);

    await drawer.getByRole('heading', { name: /The word was/ }).waitFor();
    await drawer.waitForTimeout(700); // let the result sheet and the score re-ranking settle
    if (turn === 1) await shot(bob === drawer ? alice : bob, '07-turn-result');
    log(`turn ${turn}: everyone guessed`);
  }

  await alice.getByRole('heading', { name: /wins!/ }).waitFor({ timeout: 15000 });
  await alice.waitForTimeout(1200);
  await shot(alice, '08-podium');
  await shot(cara, '09-podium-phone');
  log('game over:', await alice.locator('div.podium:not(.overlay)').innerText().then((t) => t.replace(/\s+/g, ' ')));

  // --- back to the lobby, then someone leaves
  await alice.getByText('Set up your room').waitFor({ timeout: 15000 });
  await cara.getByRole('button', { name: 'Leave' }).click();
  await cara.getByText('How to play').waitFor();
  await alice.locator('.lines').getByText('Cara left the room.').waitFor();
  log('leave works');

  // --- public quick play: two strangers get matched and the game starts by itself
  const dan = await player('Dan', { width: 1280, height: 820 });
  const eve = await player('Eve', { width: 1280, height: 820 });
  const fay = await player('Fay', { width: 1280, height: 820 });
  for (const p of [dan, eve]) {
    await p.goto(BASE);
    await p.getByPlaceholder('Enter your name').fill(p.name);
    await p.getByRole('button', { name: 'Play!' }).click();
  }
  await dan.getByText(/is choosing|Pick a word to draw/).first().waitFor();
  const code = await dan.locator('.lines').getByText(/You joined room/).innerText();
  if (code !== (await eve.locator('.lines').getByText(/You joined room/).innerText())) throw new Error('quick play split players');
  log('quick play matched Dan and Eve, game started by itself');

  // a third player joins mid-game, then the other two vote them out
  await fay.goto(BASE);
  await fay.getByPlaceholder('Enter your name').fill('Fay');
  await fay.getByRole('button', { name: 'Play!' }).click();
  await fay.getByText('Round 1 of 3').waitFor();
  for (const p of [dan, eve]) {
    await p.getByRole('region', { name: 'Players' }).locator('li', { hasText: 'Fay' }).hover();
    await p.getByRole('button', { name: 'Vote to kick Fay' }).click();
  }
  await fay.getByText('You were kicked from the room.').waitFor();
  await dan.locator('.lines').getByText('Fay was kicked.').waitFor();
  log('mid-game join and vote kick work');

  console.log('\nE2E passed. Screenshots in documents/screenshots/');
} finally {
  await browser.close();
}
