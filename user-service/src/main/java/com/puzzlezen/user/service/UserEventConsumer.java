package com.puzzlezen.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;

    /**
     * Écoute les événements "user-registered" publiés par auth-service.
     * Crée automatiquement le profil utilisateur dans user-service.
     */
    @KafkaListener(topics = "user-registered", groupId = "user-service-group")
    public void onUserRegistered(String username) {
        log.info("Événement reçu — nouvel utilisateur : {}", username);
        userService.createProfile(username);
    }
}
