package com.puzzlezen.leaderboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utilise les Redis Sorted Sets pour un classement en temps réel.
 * Clé Redis : "leaderboard:{difficulty}" (ex: "leaderboard:EASY")
 * Score Redis = score du joueur (tri décroissant via rank inversé)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "leaderboard:";
    private static final String KEY_GLOBAL = "leaderboard:GLOBAL";

    public void submitScore(String username, String difficulty, int score) {
        String key = KEY_PREFIX + difficulty;
        redisTemplate.opsForZSet().add(key, username, score);
        redisTemplate.opsForZSet().add(KEY_GLOBAL, username, score);
        log.debug("Score soumis — {} : {} pts ({})", username, score, difficulty);
    }

    /** Top N joueurs pour un niveau donné */
    public List<LeaderboardEntry> getTop(String difficulty, int limit) {
        String key = KEY_PREFIX + difficulty;
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
        return toEntries(tuples);
    }

    /** Classement global toutes difficultés confondues */
    public List<LeaderboardEntry> getGlobalTop(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(KEY_GLOBAL, 0, limit - 1);
        return toEntries(tuples);
    }

    /** Rang d'un joueur spécifique (1-based) */
    public Long getRank(String username, String difficulty) {
        String key = KEY_PREFIX + difficulty;
        Long rank = redisTemplate.opsForZSet().reverseRank(key, username);
        return rank != null ? rank + 1 : null;
    }

    private List<LeaderboardEntry> toEntries(Set<ZSetOperations.TypedTuple<String>> tuples) {
        List<LeaderboardEntry> result = new ArrayList<>();
        if (tuples == null) return result;
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            result.add(new LeaderboardEntry(rank++, t.getValue(),
                    t.getScore() != null ? t.getScore().intValue() : 0));
        }
        return result;
    }

    public record LeaderboardEntry(int rank, String username, int score) {}
}
