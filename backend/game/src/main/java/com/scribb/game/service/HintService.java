
// HintService.java
package com.scribb.game.service;

import com.scribb.game.model.GameRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintService {
    
    private final RoomManager roomManager;
    
    public String generateHint(String roomId) {
        GameRoom room = roomManager.getRoomOptional(roomId).orElse(null);
        if (room == null || room.getCurrentWord() == null) {
            return "";
        }
        
        String word = room.getCurrentWord().toUpperCase();
        long elapsedMillis = System.currentTimeMillis() - room.getTimerStartTime();
        int elapsedSeconds = (int) (elapsedMillis / 1000);
        
        // Reveal one letter every 10 seconds
        int lettersToReveal = Math.min(elapsedSeconds / 10, word.length());
        
        return buildHint(word, lettersToReveal);
    }
    
    private String buildHint(String word, int lettersToReveal) {
        // Use deterministic seed for consistent reveals
        Random random = new Random(word.hashCode());
        
        List<Integer> revealOrder = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            revealOrder.add(i);
        }
        Collections.shuffle(revealOrder, random);
        
        char[] hint = new char[word.length()];
        Arrays.fill(hint, '_');
        
        for (int i = 0; i < lettersToReveal && i < revealOrder.size(); i++) {
            int index = revealOrder.get(i);
            hint[index] = word.charAt(index);
        }
        
        return String.join(" ", new String(hint).split(""));
    }
}