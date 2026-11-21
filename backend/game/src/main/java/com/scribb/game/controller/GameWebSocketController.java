package com.scribb.game.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.scribb.game.model.Requests;
import com.scribb.game.model.RoomSettings;
import com.scribb.game.service.GameEngine;

import lombok.RequiredArgsConstructor;

/**
 * Thin STOMP adapter: maps /app/** destinations to GameEngine calls.
 * The caller is always identified by its STOMP session id, never by anything in the payload.
 * See documents/PROTOCOL.md for the full message list.
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private static final String SESSION = "simpSessionId";

    private final GameEngine engine;

    @MessageMapping("/room/quick")
    public void quickPlay(@Header(SESSION) String session, @Payload Requests.Join req) {
        engine.quickPlay(session, req);
    }

    @MessageMapping("/room/create")
    public void create(@Header(SESSION) String session, @Payload Requests.Join req) {
        engine.createPrivate(session, req);
    }

    @MessageMapping("/room/join")
    public void join(@Header(SESSION) String session, @Payload Requests.Join req) {
        engine.joinByCode(session, req);
    }

    @MessageMapping("/room/leave")
    public void leave(@Header(SESSION) String session) {
        engine.leave(session);
    }

    @MessageMapping("/room/sync")
    public void sync(@Header(SESSION) String session) {
        engine.sync(session);
    }

    @MessageMapping("/room/settings")
    public void settings(@Header(SESSION) String session, @Payload RoomSettings settings) {
        engine.updateSettings(session, settings);
    }

    @MessageMapping("/game/start")
    public void start(@Header(SESSION) String session) {
        engine.start(session);
    }

    @MessageMapping("/game/choose")
    public void choose(@Header(SESSION) String session, @Payload Requests.ChooseWord req) {
        engine.chooseWord(session, req.index());
    }

    @MessageMapping("/draw")
    public void draw(@Header(SESSION) String session, @Payload Requests.Draw req) {
        engine.draw(session, req.ops());
    }

    @MessageMapping("/draw/clear")
    public void clear(@Header(SESSION) String session) {
        engine.clear(session);
    }

    @MessageMapping("/draw/undo")
    public void undo(@Header(SESSION) String session) {
        engine.undo(session);
    }

    @MessageMapping("/chat")
    public void chat(@Header(SESSION) String session, @Payload Requests.Chat req) {
        engine.chat(session, req.text());
    }

    @MessageMapping("/rate")
    public void rate(@Header(SESSION) String session, @Payload Requests.Rate req) {
        engine.rate(session, req.like());
    }

    @MessageMapping("/kick/vote")
    public void voteKick(@Header(SESSION) String session, @Payload Requests.Target req) {
        engine.voteKick(session, req.playerId());
    }

    @MessageMapping("/kick")
    public void kick(@Header(SESSION) String session, @Payload Requests.Target req) {
        engine.ownerKick(session, req.playerId());
    }

    /** Closing the tab or losing the connection counts as leaving. */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        engine.leave(event.getSessionId());
    }
}
