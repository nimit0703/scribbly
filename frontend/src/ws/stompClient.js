import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client;
let connected = false;
let connectingPromise = null;

export function connectWebSocket(onConnectCallback) {
  if (connected && client) {
    onConnectCallback();
    return Promise.resolve();
  }

  if (connectingPromise) {
    return connectingPromise;
  }

  // Detect protocol and set the correct port dynamically
  const isHttps = window.location.protocol === 'https:';
  const port = isHttps ? 8443 : 8080;
  const protocol = isHttps ? 'https' : 'http';
  const socketUrl = `${protocol}://localhost:${port}/ws`;

  console.log(`Connecting to WebSocket via ${socketUrl}`);

  connectingPromise = new Promise((resolve, reject) => {
    const socket = new SockJS(socketUrl);

    client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        connected = true;
        connectingPromise = null;
        onConnectCallback();
        resolve();
      },
      onDisconnect: () => {
        connected = false;
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        connectingPromise = null;
        reject(new Error('STOMP connection failed'));
      },
      debug: () => {}, // optional logging
    });

    client.activate();
  });

  return connectingPromise;
}

export function getStompClient() {
  return client;
}

export function isConnected() {
  return connected;
}

export function disconnect() {
  if (client) {
    client.deactivate();
    connected = false;
  }
}

// export default StompClient;
