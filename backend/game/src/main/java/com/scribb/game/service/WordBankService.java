package com.scribb.game.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.scribb.game.config.GameProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordBankService {

    private final GameProperties props;
    private List<String> words = List.of();

    @PostConstruct
    void load() {
        var resource = getClass().getClassLoader().getResourceAsStream(props.getDictionaryFile());
        if (resource == null) throw new IllegalStateException(props.getDictionaryFile() + " not found on classpath");
        try (var br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            var set = new LinkedHashSet<String>();
            br.lines().map(l -> l.trim().toLowerCase())
                    .filter(l -> !l.isBlank() && !l.startsWith("#") && l.length() <= 30)
                    .forEach(set::add);
            words = List.copyOf(set);
            log.info("Loaded {} words", words.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load words", e);
        }
    }

    /**
     * n distinct words for the drawer to choose from.
     * Custom words are mixed into the built-in list, or used alone when customOnly and there are enough of them.
     */
    public List<String> choices(int n, List<String> custom, boolean customOnly) {
        List<String> pool;
        if (customOnly && custom.size() >= n) {
            pool = new ArrayList<>(custom);
        } else {
            var set = new LinkedHashSet<>(words);
            set.addAll(custom);
            pool = new ArrayList<>(set);
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return List.copyOf(pool.subList(0, Math.min(n, pool.size())));
    }

    public int size() {
        return words.size();
    }
}
