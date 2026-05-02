package com.puzzlezen.leaderboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardEventConsumer {

    private final LeaderboardService leaderboardService;
    private final ObjectMapper objectMapper;

    /**
     * Écoute le topic "game-completed" publié par game-service.
     * Payload attendu : { "username": "...", "difficulty": "EASY", "score": 120 }
     */
    @KafkaListener(topics = "game-completed", groupId = "leaderboard-service-group")
    public void onGameCompleted(String payload) {
        try {
            Map<?, ?> event = objectMapper.readValue(payload, Map.class);
            String username = (String) event.get("username");
            String difficulty = (String) event.get("difficulty");
            int score = (int) event.get("score");
            leaderboardService.submitScore(username, difficulty, score);
            log.info("Leaderboard mis à jour — {} : {} pts ({})", username, score, difficulty);
        } catch (Exception e) {
            log.error("Erreur traitement événement game-completed : {}", e.getMessage());
        }
    }
}
