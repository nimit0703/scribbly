package com.scribb.game.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    private String username;
    private int score = 0;
    private boolean hasGuessedCorrectly = false;
}