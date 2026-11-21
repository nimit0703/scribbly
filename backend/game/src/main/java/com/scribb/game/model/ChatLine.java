package com.scribb.game.model;

/**
 * A line in the chat box. kind drives the colour on the client:
 * chat (normal), guessed (only seen by people who know the word), system, success, warn, close.
 */
public record ChatLine(String kind, String playerId, String name, String text) {
    public static ChatLine chat(Player p, String text) { return new ChatLine("chat", p.getId(), p.getName(), text); }
    public static ChatLine guessed(Player p, String text) { return new ChatLine("guessed", p.getId(), p.getName(), text); }
    public static ChatLine system(String text) { return new ChatLine("system", null, null, text); }
    public static ChatLine success(String text) { return new ChatLine("success", null, null, text); }
    public static ChatLine warn(String text) { return new ChatLine("warn", null, null, text); }
    public static ChatLine close(String text) { return new ChatLine("close", null, null, text); }
}
