package com.scribb.game.model;

import java.util.Map;

/** Shown to everyone between turns: the word, who gained what, and why the turn ended. */
public record TurnResult(String word, Map<String, Integer> gains, String reason) {
}
