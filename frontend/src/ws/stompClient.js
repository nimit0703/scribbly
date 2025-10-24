import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client;
let connected = false;
let connectingPromise = null;

export function connectWebSocket(onConnectCallback) {
  // If already connected, just call the callback
  if (connected && client) {
    onConnectCallback();
    return Promise.resolve();
  }

  // If connection is in progress, return the existing promise
  if (connectingPromise) {
    return connectingPromise;
  }

  // Create new connection
  connectingPromise = new Promise((resolve, reject) => {
    const socket = new SockJS('http://localhost:8080/ws');
    
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
      debug: () => {} // optional logging
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