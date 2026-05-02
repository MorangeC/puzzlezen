package com.puzzlezen.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Reçoit un événement Kafka "game-completed" et le broadcast
     * via WebSocket sur /topic/leaderboard pour rafraîchir le classement
     * en temps réel chez tous les clients connectés.
     */
    @KafkaListener(topics = "game-completed", groupId = "notification-service-group")
    public void onGameCompleted(String payload) {
        try {
            Map<?, ?> event = objectMapper.readValue(payload, Map.class);
            messagingTemplate.convertAndSend("/topic/leaderboard", event);
            log.info("Notification broadcast — game-completed : {}", event.get("username"));
        } catch (Exception e) {
            log.error("Erreur notification game-completed : {}", e.getMessage());
        }
    }

    /** Notifie un joueur spécifique (ex: timer expiré) */
    public void notifyUser(String username, String type, Object payload) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications",
                Map.of("type", type, "payload", payload));
    }
}
