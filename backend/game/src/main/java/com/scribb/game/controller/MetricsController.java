package com.scribb.game.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scribb.game.model.matrics.GameMetrics;
import com.scribb.game.service.monitoring.MonitoringService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final GameMetrics gameMetrics;
    private final MonitoringService monitoringService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Player metrics
        summary.put("activePlayers", gameMetrics.getActivePlayers().get());
        summary.put("activeRooms", gameMetrics.getActiveRooms().get());
        summary.put("totalPlayerJoins", gameMetrics.getPlayerJoins().count());
        summary.put("totalPlayerLeaves", gameMetrics.getPlayerLeaves().count());

        // Game metrics
        summary.put("totalRoundStarts", gameMetrics.getRoundStarts().count());
        summary.put("totalRoundEnds", gameMetrics.getRoundEnds().count());
        summary.put("ongoingGames", gameMetrics.getOngoingGames().get());

        // Guess metrics
        summary.put("correctGuesses", gameMetrics.getCorrectGuesses().count());
        summary.put("incorrectGuesses", gameMetrics.getIncorrectGuesses().count());
        summary.put("guessAccuracy", gameMetrics.getGuessAccuracy());

        // Activity metrics
        summary.put("totalDrawEvents", gameMetrics.getDrawEvents().count());
        summary.put("totalChatMessages", gameMetrics.getChatMessages().count());
        summary.put("totalHintRequests", gameMetrics.getHintRequests().count());

        // Error metrics
        summary.put("totalErrors", gameMetrics.getErrors().count());
        summary.put("errorRate", gameMetrics.getErrorRate());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomMetrics(@PathVariable String roomId) {
        MonitoringService.RoomMetrics roomMetrics = monitoringService.getRoomMetrics(roomId);

        if (roomMetrics == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("roomId", roomMetrics.getRoomId());
        metrics.put("playerCount", roomMetrics.getPlayerCount());
        metrics.put("roundsStarted", roomMetrics.getRoundsStarted());
        metrics.put("roundsCompleted", roomMetrics.getRoundsCompleted());
        metrics.put("correctGuesses", roomMetrics.getCorrectGuesses());
        metrics.put("incorrectGuesses", roomMetrics.getIncorrectGuesses());
        metrics.put("guessAccuracy", roomMetrics.getGuessAccuracy());
        metrics.put("roundCompletionRate", roomMetrics.getRoundCompletionRate());
        metrics.put("totalPointsAwarded", roomMetrics.getPointsAwarded());
        metrics.put("createdAt", roomMetrics.getCreatedAt());

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getAllRooms() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalActiveRooms", monitoringService.getTotalActiveRooms());
        response.put("totalActivePlayers", monitoringService.getTotalActivePlayers());
        response.put("rooms", monitoringService.getAllRoomMetrics());

        return ResponseEntity.ok(response);
    }
}
