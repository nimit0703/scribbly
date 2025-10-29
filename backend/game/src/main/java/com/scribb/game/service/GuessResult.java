// GuessResult.java
package com.scribb.game.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuessResult {
    private final boolean correct;
    private final boolean roundComplete;
    private final int pointsAwarded;
    private final String message;
    
    public static GuessResult correct(int points) {
        return new GuessResult(true, false, points, "Correct!");
    }
    
    public static GuessResult correctAndRoundComplete(int points) {
        return new GuessResult(true, true, points, "Round complete!");
    }
    
    public static GuessResult incorrect() {
        return new GuessResult(false, false, 0, "Incorrect");
    }
    
    public static GuessResult invalid(String reason) {
        return new GuessResult(false, false, 0, reason);
    }
    
    public static GuessResult alreadyGuessed() {
        return new GuessResult(false, false, 0, "Already guessed");
    }
}