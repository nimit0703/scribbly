package com.scribb.game.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.scribb.game.model.GameRoom;
import com.scribb.game.model.Player;

import jakarta.annotation.PostConstruct;

@Service
public class RoomManager {
    @PostConstruct
    public void init() {
        System.out.println("_____________________ RoomManager initialized ______________________________________________");
    }
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final List<String> wordBank = List.of("apple", "carrot", "house", "banana", "computer", "pizza");

    private final GameRoundService gameRoundService;
    private final TimerService timerService;
    private final WordBankService wordBankService;

    public RoomManager(@Lazy GameRoundService gameRoundService, @Lazy TimerService timerService,@Lazy WordBankService wordBankService) {
        this.gameRoundService = gameRoundService;
        this.timerService = timerService;
        this.wordBankService = wordBankService;
    }

    public GameRoom createOrJoinRoom(String roomId, String username) {
        GameRoom room = rooms.computeIfAbsent(roomId, GameRoom::new);
        if (room.getPlayer(username).isEmpty()) {
            room.getPlayers().add(new Player(username, 0, false));
        }
        return room;
    }

    public List<String> getRandomWords() {

        return wordBankService.takeRandom(3);
    }

    public void setWord(String roomId, String drawer, String word) {
        GameRoom room = rooms.get(roomId);
        
        if (room != null) {
            String currentDrawer = room.getCurrentDrawer();
            if (currentDrawer != null) {
                room.setCurrentWord(word);
                room.setCurrentDrawer(drawer);
                room.setRoundStartTime(System.currentTimeMillis());
                System.out.println( "word is set for room" + room.getRoomId() + " :" +room.getCurrentWord());
                timerService.startTimer(roomId);
                System.out.println( "Timer started for roomID" + roomId +" time left : "+ room.getRemainingTimeSeconds());
            }
        }
    }

    public boolean processGuess(String roomId, String username, String guess) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.getCurrentWord() == null || username.equals(room.getCurrentDrawer())) {
            return false;
        }
        System.out.println(room.getCurrentWord());
        System.out.println(username.equals(room.getCurrentDrawer()));

        if (guess.trim().equalsIgnoreCase(room.getCurrentWord())) {
            if (!room.getCorrectGuessers().contains(username)) {
                long timeTaken = System.currentTimeMillis() - room.getRoundStartTime();
                int bonus = (int) Math.max(10, 100 - (timeTaken / 1000));
                room.getPlayer(username).ifPresent(player -> {
                    player.setHasGuessedCorrectly(true);
                    player.setScore(player.getScore() + bonus);
                });
                room.getPlayer(room.getCurrentDrawer()).ifPresent(drawer -> {
                    drawer.setScore(drawer.getScore() + 10); // drawer bonus
                });
                room.getCorrectGuessers().add(username);
                return true;
            }
        }

        return false;
    }

    public void endRound(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.resetForNextRound();
        }
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public boolean isRoundOver(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) return true;

        long now = System.currentTimeMillis();
        boolean timeExpired = (now - room.getRoundStartTime()) > 60_000; // 60 sec round
        boolean allGuessed = room.getPlayers().stream()
                .filter(p -> !p.getUsername().equals(room.getCurrentDrawer()))
                .allMatch(Player::isHasGuessedCorrectly);

        return timeExpired || allGuessed;
    }

    public String pickNextDrawer(GameRoom room) {
        List<Player> players = room.getPlayers();
        if (players.isEmpty()) return null;

        int nextIndex = (room.getLastDrawerIndex() + 1) % players.size();
        room.setLastDrawerIndex(nextIndex);

        String drawer = players.get(nextIndex).getUsername();
        System.out.println("Next drawer selected: " + drawer); // ✅ debug log
        return drawer;
    }
    
    // Hint generation method
    // at start of game return _ _ _ like lenghth of word
    // then randome unhidden letters at intervals 10 seconds
    public String generateHintForRoom(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null || room.getCurrentWord() == null) {
            return "";
        }
        String word = room.getCurrentWord();
        long elapsedMillis = System.currentTimeMillis() - room.getTimerStartTime(); 
        int elapsedSeconds = (int) (elapsedMillis / 1000);
        int lettersToReveal = Math.min(elapsedSeconds / 10, word.length()); 
        StringBuilder hint = new StringBuilder("_".repeat(word.length()));
        for (int i = 0; i < lettersToReveal; i++) {
            int index;
            do {
                index = (int) (Math.random() * word.length());
            } while (hint.charAt(index) != '_');
            hint.setCharAt(index, word.charAt(index));
        }
        return hint.toString();
    }
    @Scheduled(fixedRate = 5000)
    public void checkRoundTimeouts(){
        long now = System.currentTimeMillis();
        for (GameRoom room : rooms.values()) {
            if (room.getCurrentWord() != null && !room.isGameOver()) {
                long elapsed = now - room.getRoundStartTime();

                if (elapsed >= room.getRoundDurationMillis()) {
                    // Time's up! End round.
                    System.out.println("⏰ Round timeout for room: " + room.getRoomId());
                    gameRoundService.endRound(room.getRoomId());
                }
            }
        }
    }
}
