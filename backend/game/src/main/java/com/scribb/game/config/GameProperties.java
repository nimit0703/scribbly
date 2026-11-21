package com.scribb.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/** Server-wide timings. Per-room options (rounds, draw time, hints...) live in RoomSettings. */
@Data
@Configuration
@ConfigurationProperties(prefix = "game")
public class GameProperties {
    /** Seconds the drawer has to pick a word before one is picked for them. */
    private int chooseSeconds = 15;
    /** Seconds the "the word was..." screen stays up between turns. */
    private int turnEndSeconds = 6;
    /** Seconds the podium stays up before the room goes back to the lobby. */
    private int gameOverSeconds = 10;
    private int publicMaxPlayers = 8;
    private String dictionaryFile = "static/words.txt";
    /** Origin patterns allowed to open the WebSocket. Tighten this in production. */
    private String[] allowedOrigins = {"*"};
}
