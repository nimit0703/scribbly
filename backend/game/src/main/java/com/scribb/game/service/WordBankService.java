package com.scribb.game.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WordBankService {
    private final List<String > words = new CopyOnWriteArrayList<>();

    @PostConstruct
    void load(){
        var resource = getClass().getClassLoader().getResourceAsStream("static/words.txt");

        if (resource == null){
            throw new IllegalStateException("words.txt not exist");
        }
        try(var br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))){
            br.lines()
                    .map(String::trim)
                    .filter(line-> !line.isBlank() && !line.startsWith("#"))
                    .forEach(words::add);
        }catch (Exception e){
            throw  new RuntimeException("Failed to load words",e);
        }
        System.out.printf("✅ Loaded %d words into word bank%n", words.size());
    }

    public List<String> takeRandom(int n) {
        if (words.size() < n) {
            throw new IllegalStateException("Not enough words in bank");
        }
        List<String> copy = new ArrayList<>(words);
        Collections.shuffle(copy);
        return copy.subList(0, n);
    }

    /** Optional helper so you can add new words at runtime if you like. */
    public void addWord(String newWord) {
        words.add(Objects.requireNonNull(newWord).trim().toLowerCase());
    }
}
