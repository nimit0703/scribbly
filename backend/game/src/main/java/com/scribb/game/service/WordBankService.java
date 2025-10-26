package com.scribb.game.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WordBankService {
    
    private final List<String> words = new CopyOnWriteArrayList<>();
    private volatile boolean loaded = false;

    @PostConstruct
    void load() {
        if (loaded) {
            return;
        }
        
        var resource = getClass().getClassLoader().getResourceAsStream("static/words.txt");

        if (resource == null) {
            log.error("words.txt not found in resources");
            throw new IllegalStateException("words.txt does not exist");
        }
        
        try (var br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            br.lines()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .filter(word -> word.length() >= 3 && word.length() <= 15)
                .forEach(words::add);
                
            loaded = true;
            log.info("✅ Loaded {} words into word bank", words.size());
            
        } catch (Exception e) {
            log.error("Failed to load words from file", e);
            throw new RuntimeException("Failed to load words", e);
        }
    }

    public List<String> takeRandom(int n) {
        if (!loaded) {
            throw new IllegalStateException("Word bank not loaded yet");
        }
        
        if (words.size() < n) {
            throw new IllegalStateException(
                "Not enough words in bank. Required: " + n + ", Available: " + words.size()
            );
        }
        
        // Use ThreadLocalRandom for better performance in concurrent scenarios
        Set<String> selected = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        while (selected.size() < n) {
            int index = random.nextInt(words.size());
            selected.add(words.get(index));
        }
        
        return new ArrayList<>(selected);
    }
    
    public int getWordCount() {
        return words.size();
    }

    public void addWord(String newWord) {
        if (newWord != null && !newWord.isBlank()) {
            String word = newWord.trim().toLowerCase();
            if (!words.contains(word)) {
                words.add(word);
                log.info("Added new word to bank: {}", word);
            }
        }
    }
    
    public boolean containsWord(String word) {
        return word != null && words.contains(word.trim().toLowerCase());
    }
}