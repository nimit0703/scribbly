import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// In dev, Vite proxies /ws to Spring Boot. Point VITE_WS_URL at the server when hosting separately.
const WS_URL = import.meta.env.VITE_WS_URL || '/ws';

let client = null;
let ready = null;

/**
 * Opens (once) the STOMP connection. Resolves with the client.
 * No auto-reconnect: the server identifies players by connection, so a new connection is a new player.
 * onClose fires when an established connection drops.
 */
export function connect(onClose) {
  if (ready) return ready;
  ready = new Promise((resolve, reject) => {
    let opened = false;
    client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 0,
      onConnect: () => {
        opened = true;
        resolve(client);
      },
      onStompError: (frame) => reject(new Error(frame.headers.message || 'Connection error')),
      onWebSocketClose: () => {
        ready = null;
        if (opened) onClose?.();
        else reject(new Error("Can't reach the game server. Is it running?"));
      },
    });
    client.activate();
  });
  return ready;
}

export function disconnect() {
  client?.deactivate();
  ready = null;
}
