import { describe, it, expect, vi } from 'vitest';

// The store module touches the socket and audio only inside actions; stub them for a pure reducer test.
vi.mock('../../ws/stompClient.js', () => ({ connect: vi.fn(), disconnect: vi.fn() }));
vi.mock('../sound.js', () => ({ play: vi.fn() }));
globalThis.localStorage = { getItem: () => null, setItem: () => {} };

const { reduce, freshState } = await import('../store.js');

const room = (over = {}) => ({
  code: 'ABC123', phase: 'LOBBY', drawerId: null, timeLeft: 0, players: [], settings: { rounds: 3 }, ...over,
});

describe('reduce', () => {
  it('welcome loads you, the room and the drawing', () => {
    const s = freshState();
    const fx = reduce(s, { type: 'welcome', you: 'p1', state: room({ timeLeft: 10 }), ops: [{ t: 's' }], word: 'cat' }, 1000);
    expect(s.you).toBe('p1');
    expect(s.room.code).toBe('ABC123');
    expect(s.word).toBe('cat');
    expect(s.deadline).toBe(11_000);
    expect(fx.canvas).toEqual(['sync', [{ t: 's' }]]);
  });

  it('a new turn forgets the old word and choices', () => {
    const s = freshState();
    reduce(s, { type: 'welcome', you: 'p1', state: room({ phase: 'TURN_END' }), word: 'cat', words: [] });
    reduce(s, { type: 'state', state: room({ phase: 'CHOOSING', drawerId: 'p2' }) });
    expect(s.word).toBeNull();
    const fx = reduce(s, { type: 'state', state: room({ phase: 'DRAWING', drawerId: 'p2' }) });
    expect(fx.sound).toBe('start');
  });

  it('the turn result reveals the word to everyone', () => {
    const s = freshState();
    reduce(s, { type: 'welcome', you: 'p1', state: room({ phase: 'DRAWING' }) });
    reduce(s, { type: 'state', state: room({ phase: 'TURN_END', turnResult: { word: 'moon', gains: {} } }) });
    expect(s.word).toBe('moon');
  });

  it('draw ops are applied for viewers but not echoed back to the drawer', () => {
    const viewer = freshState();
    reduce(viewer, { type: 'welcome', you: 'p1', state: room({ phase: 'DRAWING', drawerId: 'p2' }) });
    expect(reduce(viewer, { type: 'draw', ops: [{ t: 's' }] }).canvas).toEqual(['ops', [{ t: 's' }]]);
    expect(viewer.ops).toHaveLength(1);

    const drawer = freshState();
    reduce(drawer, { type: 'welcome', you: 'p2', state: room({ phase: 'DRAWING', drawerId: 'p2' }) });
    expect(reduce(drawer, { type: 'draw', ops: [{ t: 's' }] }).canvas).toBeNull();
  });

  it('sync replaces the whole drawing (undo/clear)', () => {
    const s = freshState();
    s.ops = [{ t: 's' }, { t: 's' }];
    expect(reduce(s, { type: 'sync', ops: [] }).canvas).toEqual(['sync', []]);
    expect(s.ops).toEqual([]);
  });

  it('chat is capped and correct guesses play a sound', () => {
    const s = freshState();
    for (let i = 0; i < 200; i++) reduce(s, { type: 'chat', line: { kind: 'chat', text: `${i}` } });
    expect(s.chat).toHaveLength(150);
    expect(s.chat[0].text).toBe('50');
    expect(reduce(s, { type: 'chat', line: { kind: 'success', text: 'Bob guessed the word!' } }).sound).toBe('guess');
  });

  it('errors and kicks are recorded', () => {
    const s = freshState();
    reduce(s, { type: 'error', message: 'That room is full.' });
    reduce(s, { type: 'kicked' });
    expect(s.error).toBe('That room is full.');
    expect(s.kicked).toBe(true);
  });
});
