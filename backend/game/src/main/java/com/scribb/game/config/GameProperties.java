package com.scribb.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "game")
public class GameProperties {
    
    private RoundConfig round = new RoundConfig();
    private ScoringConfig scoring = new ScoringConfig();
    private TimerConfig timer = new TimerConfig();
    private WordConfig word = new WordConfig();
    
    @Data
    public static class RoundConfig {
        private int durationSeconds = 60;
        private int minPlayers = 2;
        private int maxPlayers = 8;
        private int totalRounds = 3;
    }
    
    @Data
    public static class ScoringConfig {
        private int maxBonus = 100;
        private int minBonus = 10;
        private int drawerBonusPerGuesser = 10;
    }
    
    @Data
    public static class TimerConfig {
        private int broadcastIntervalMs = 1000;
        private int hintIntervalSeconds = 10;
    }
    
    @Data
    public static class WordConfig {
        private int optionsCount = 3;
        private String dictionaryFile = "static/words.txt";
    }
}