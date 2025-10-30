package com.scribb.game.model.matrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;

@Component
public class GameMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> counters;
    private final ConcurrentHashMap<String, AtomicInteger> gauges;

    // Counters
    @Getter
    private Counter playerJoins;
    @Getter
    private Counter playerLeaves;
    @Getter
    private Counter roundStarts;
    @Getter
    private Counter roundEnds;
    @Getter
    private Counter correctGuesses;
    @Getter
    private Counter incorrectGuesses;
    @Getter
    private Counter wordSelections;
    @Getter
    private Counter drawEvents;
    @Getter
    private Counter drawSessions;
    @Getter
    private Counter canvasClears;
    @Getter
    private Counter chatMessages;
    @Getter
    private Counter hintRequests;
    @Getter
    private Counter timerEndedRounds;
    @Getter
    private Counter errors;

    // Gauges
    @Getter
    private AtomicInteger activeRooms;
    @Getter
    private AtomicInteger activePlayers;
    @Getter
    private AtomicInteger ongoingGames;

    // Timers
    @Getter
    private Timer roundDurationTimer;
    @Getter
    private Timer guessResponseTimer;
    @Getter
    private Timer drawingSessionTimer;

    public GameMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.counters = new ConcurrentHashMap<>();
        this.gauges = new ConcurrentHashMap<>();
        initializeMetrics();
    }

    private void initializeMetrics() {
        // Initialize counters
        playerJoins = createCounter("player.joins", "Total player joins");
        playerLeaves = createCounter("player.leaves", "Total player leaves");
        roundStarts = createCounter("round.starts", "Total round starts");
        roundEnds = createCounter("round.ends", "Total round ends");
        correctGuesses = createCounter("guesses.correct", "Total correct guesses");
        incorrectGuesses = createCounter("guesses.incorrect", "Total incorrect guesses");
        wordSelections = createCounter("word.selections", "Total word selections");
        drawEvents = createCounter("draw.events", "Total draw events");
        drawSessions = createCounter("draw.sessions", "Total draw sessions");
        canvasClears = createCounter("canvas.clears", "Total canvas clears");
        chatMessages = createCounter("chat.messages", "Total chat messages");
        hintRequests = createCounter("hint.requests", "Total hint requests");
        timerEndedRounds = createCounter("round.timer_ended", "Rounds ended by timer");
        errors = createCounter("errors.total", "Total errors");

        // Initialize gauges
        activeRooms = createGauge("rooms.active", "Active rooms count");
        activePlayers = createGauge("players.active", "Active players count");
        ongoingGames = createGauge("games.ongoing", "Ongoing games count");

        // Initialize timers
        roundDurationTimer = Timer.builder("round.duration")
                .description("Round duration distribution")
                .register(meterRegistry);

        guessResponseTimer = Timer.builder("guess.response.time")
                .description("Time taken to process guesses")
                .register(meterRegistry);

        drawingSessionTimer = Timer.builder("drawing.session.time")
                .description("Drawing session duration")
                .register(meterRegistry);
    }

    private Counter createCounter(String name, String description) {
        Counter counter = Counter.builder("game." + name)
                .description(description)
                .register(meterRegistry);
        counters.put(name, counter);
        return counter;
    }

    private AtomicInteger createGauge(String name, String description) {
        AtomicInteger gauge = new AtomicInteger(0);
        Gauge.builder("game." + name, gauge, AtomicInteger::get)
                .description(description)
                .register(meterRegistry);
        gauges.put(name, gauge);
        return gauge;
    }

    // Counter increment methods
    public void incrementPlayerJoins() {
        playerJoins.increment();
    }

    public void incrementPlayerLeaves() {
        playerLeaves.increment();
    }

    public void incrementRoundStarts() {
        roundStarts.increment();
    }

    public void incrementRoundEnds() {
        roundEnds.increment();
    }

    public void incrementCorrectGuesses() {
        correctGuesses.increment();
    }

    public void incrementIncorrectGuesses() {
        incorrectGuesses.increment();
    }

    public void incrementWordSelections() {
        wordSelections.increment();
    }

    public void incrementDrawEvents() {
        drawEvents.increment();
    }

    public void incrementDrawSessions() {
        drawSessions.increment();
    }

    public void incrementCanvasClears() {
        canvasClears.increment();
    }

    public void incrementChatMessages() {
        chatMessages.increment();
    }

    public void incrementHintRequests() {
        hintRequests.increment();
    }

    public void incrementTimerEndedRounds() {
        timerEndedRounds.increment();
    }

    public void incrementErrors() {
        errors.increment();
    }

    public void incrementErrors(String errorType) {
        errors.increment();
        Counter errorTypeCounter = counters.computeIfAbsent("errors." + errorType, 
            key -> createCounter("errors." + errorType, "Errors of type: " + errorType));
        errorTypeCounter.increment();
    }

    // Gauge update methods
    public void setActiveRooms(int count) {
        activeRooms.set(count);
    }

    public void setActivePlayers(int count) {
        activePlayers.set(count);
    }

    public void setOngoingGames(int count) {
        ongoingGames.set(count);
    }

    // Timer record methods
    public void recordRoundDuration(long duration, TimeUnit unit) {
        roundDurationTimer.record(duration, unit);
    }

    public void recordGuessResponseTime(long duration, TimeUnit unit) {
        guessResponseTimer.record(duration, unit);
    }

    public void recordDrawingSessionTime(long duration, TimeUnit unit) {
        drawingSessionTimer.record(duration, unit);
    }

    // Utility methods
    public double getErrorRate() {
        double totalOperations = playerJoins.count() + chatMessages.count() + roundStarts.count();
        return totalOperations > 0 ? errors.count() / totalOperations : 0.0;
    }

    public double getGuessAccuracy() {
        double totalGuesses = correctGuesses.count() + incorrectGuesses.count();
        return totalGuesses > 0 ? correctGuesses.count() / totalGuesses : 0.0;
    }

    public void reset() {
        // Note: Counters and timers typically don't reset in production
        // This is mainly for testing
        activeRooms.set(0);
        activePlayers.set(0);
        ongoingGames.set(0);
    }
}