package com.puzzlezen.user.service;

import com.puzzlezen.user.model.GameResult;
import com.puzzlezen.user.model.UserProfile;
import com.puzzlezen.user.repository.GameResultRepository;
import com.puzzlezen.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository profileRepository;
    private final GameResultRepository gameResultRepository;

    /** Crée un profil vide lors du premier login (appelé via Kafka depuis auth-service) */
    @Transactional
    public UserProfile createProfile(String username) {
        if (profileRepository.existsByUsername(username)) {
            return profileRepository.findByUsername(username).orElseThrow();
        }
        UserProfile profile = UserProfile.builder().username(username).build();
        log.info("Nouveau profil créé pour : {}", username);
        return profileRepository.save(profile);
    }

    public UserProfile getProfile(String username) {
        return profileRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Profil introuvable : " + username));
    }

    /** Enregistre un résultat de jeu et met à jour les stats du joueur */
    @Transactional
    public GameResult saveResult(String username, String gameId, String gameType,
                                  String difficulty, int score, int durationSeconds, boolean completed) {
        GameResult result = GameResult.builder()
                .username(username)
                .gameId(gameId)
                .gameType(gameType)
                .difficulty(difficulty)
                .score(score)
                .durationSeconds(durationSeconds)
                .completed(completed)
                .build();

        gameResultRepository.save(result);

        // Mise à jour des stats du profil
        profileRepository.findByUsername(username).ifPresent(profile -> {
            profile.setGamesPlayed(profile.getGamesPlayed() + 1);
            if (completed) {
                profile.setGamesWon(profile.getGamesWon() + 1);
                profile.setTotalScore(profile.getTotalScore() + score);
            }
            profile.setLastPlayedAt(LocalDateTime.now(ZoneId.of("UTC")));
            profileRepository.save(profile);
        });

        return result;
    }

    public List<GameResult> getHistory(String username) {
        return gameResultRepository.findByUsernameOrderByPlayedAtDesc(username);
    }
}
