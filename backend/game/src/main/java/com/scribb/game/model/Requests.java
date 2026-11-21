package com.scribb.game.model;

import java.util.List;

/** Inbound STOMP payloads. Identity always comes from the STOMP session, never from these. */
public final class Requests {
    private Requests() {}

    public record Join(String name, Avatar avatar, String code) {}
    public record ChooseWord(int index) {}
    public record Draw(List<DrawOp> ops) {}
    public record Chat(String text) {}
    public record Target(String playerId) {}
    public record Rate(boolean like) {}
}
