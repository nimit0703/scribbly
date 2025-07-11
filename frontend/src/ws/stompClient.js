import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let client;
let connected = false;

export function connectWebSocket(onConnectCallback) {
  const socket = new SockJS('https://m5kg94rx-8080.inc1.devtunnels.ms/ws');
  client = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: onConnectCallback,
    debug: () => {} // optional logging
  });
  client.activate();
}

export function getStompClient() {
  return client;
}
