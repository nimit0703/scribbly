package com.scribb.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/** Two real STOMP clients over SockJS play one turn against the running server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameApplicationTests {

    @LocalServerPort
    int port;

    WebSocketStompClient stomp;

    /** One connected player: everything it receives, from its room topic and private queue, lands in inbox. */
    class Client {
        final StompSession session;
        final LinkedBlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        /** Every message ever received, for "this never arrived" checks. */
        final List<Map<String, Object>> seen = new java.util.concurrent.CopyOnWriteArrayList<>();

        Client() throws Exception {
            session = stomp.connectAsync("http://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);
            subscribe("/user/queue/me");
        }

        void subscribe(String dest) {
            session.subscribe(dest, new StompFrameHandler() {
                @Override public Type getPayloadType(StompHeaders headers) { return Map.class; }
                @SuppressWarnings("unchecked")
                @Override public void handleFrame(StompHeaders headers, Object payload) {
                    inbox.add((Map<String, Object>) payload);
                    seen.add((Map<String, Object>) payload);
                }
            });
        }

        void send(String dest, Object body) {
            session.send(dest, body);
        }

        /** Waits for the first message matching the predicate, skipping others. */
        Map<String, Object> await(Predicate<Map<String, Object>> match) throws InterruptedException {
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                Map<String, Object> m = inbox.poll(100, TimeUnit.MILLISECONDS);
                if (m != null && match.test(m)) return m;
            }
            throw new AssertionError("timed out waiting for message");
        }
    }

    static Predicate<Map<String, Object>> type(String t) {
        return m -> t.equals(m.get("type"));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> state(Map<String, Object> m) {
        return (Map<String, Object>) m.get("state");
    }

    static Predicate<Map<String, Object>> phase(String p) {
        return m -> "state".equals(m.get("type")) && p.equals(state(m).get("phase"));
    }

    @AfterEach
    void tearDown() {
        if (stomp != null) stomp.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoPlayersPlayATurnOverWebSocket() throws Exception {
        stomp = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        Client alice = new Client();
        Client bob = new Client();
        Thread.sleep(200); // let the private-queue subscriptions register

        alice.send("/app/room/create", Map.of("name", "Alice", "avatar", Map.of("color", 1, "eyes", 2, "mouth", 3)));
        Map<String, Object> welcome = alice.await(type("welcome"));
        String code = (String) state(welcome).get("code");
        assertThat(state(welcome).get("isPrivate")).isEqualTo(true);
        alice.subscribe("/topic/room/" + code);

        bob.send("/app/room/join", Map.of("name", "Bob", "code", code));
        assertThat(state(bob.await(type("welcome"))).get("players")).asList().hasSize(2);
        bob.subscribe("/topic/room/" + code);
        Thread.sleep(200);

        alice.send("/app/game/start", Map.of());
        List<String> words = (List<String>) alice.await(type("words")).get("words");
        bob.await(phase("CHOOSING"));

        alice.send("/app/game/choose", Map.of("index", 0));
        String word = (String) alice.await(type("word")).get("word");
        assertThat(word).isEqualTo(words.get(0));
        Map<String, Object> drawing = bob.await(phase("DRAWING"));
        assertThat((String) state(drawing).get("hint")).hasSize(word.length()).doesNotContain(word);

        alice.send("/app/draw", Map.of("ops", List.of(Map.of("t", "s", "g", 1, "c", "#112233", "s", 6, "p", List.of(10, 10, 50, 50)))));
        Map<String, Object> ops = bob.await(type("draw"));
        assertThat((List<?>) ops.get("ops")).hasSize(1);

        // bob can't wipe the canvas by publishing straight to the room topic
        alice.seen.clear();
        bob.send("/topic/room/" + code, Map.of("type", "sync", "ops", List.of()));

        bob.send("/app/chat", Map.of("text", word));
        assertThat(bob.await(type("word")).get("word")).isEqualTo(word);
        Map<String, Object> end = bob.await(phase("TURN_END"));
        Map<String, Object> result = (Map<String, Object>) state(end).get("turnResult");
        assertThat(result.get("word")).isEqualTo(word);
        alice.await(phase("TURN_END"));
        assertThat(alice.seen).noneMatch(type("sync"));

        // leaving is broadcast
        bob.session.disconnect();
        Map<String, Object> after = alice.await(m -> "state".equals(m.get("type")) && ((List<?>) state(m).get("players")).size() == 1);
        assertThat(state(after).get("phase")).isEqualTo("GAME_OVER");
    }
}
