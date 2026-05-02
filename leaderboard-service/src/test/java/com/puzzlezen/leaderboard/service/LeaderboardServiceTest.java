package com.puzzlezen.leaderboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService — tests unitaires")
class LeaderboardServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOps;
    @InjectMocks private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @Test
    @DisplayName("submitScore appelle Redis ZADD pour le niveau et le global")
    void shouldCallZAddForDifficultyAndGlobal() {
        leaderboardService.submitScore("morgan", "EASY", 500);

        verify(zSetOps).add("leaderboard:EASY", "morgan", 500.0);
        verify(zSetOps).add("leaderboard:GLOBAL", "morgan", 500.0);
    }

    @Test
    @DisplayName("getTop retourne les entrées triées avec leur rang")
    void shouldReturnTopEntriesWithRank() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(mockTuple("alice", 1000.0));
        tuples.add(mockTuple("morgan", 800.0));
        tuples.add(mockTuple("bob", 600.0));

        when(zSetOps.reverseRangeWithScores("leaderboard:EASY", 0, 9)).thenReturn(tuples);

        List<LeaderboardService.LeaderboardEntry> result = leaderboardService.getTop("EASY", 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).username()).isEqualTo("alice");
        assertThat(result.get(0).score()).isEqualTo(1000);
        assertThat(result.get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("getTop retourne une liste vide si Redis retourne null")
    void shouldReturnEmptyIfRedisReturnsNull() {
        when(zSetOps.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(null);

        List<LeaderboardService.LeaderboardEntry> result = leaderboardService.getTop("HARD", 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRank retourne le rang 1-based")
    void shouldReturnOneBbasedRank() {
        when(zSetOps.reverseRank("leaderboard:EASY", "morgan")).thenReturn(0L); // 0-based → rang 1

        Long rank = leaderboardService.getRank("morgan", "EASY");

        assertThat(rank).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRank retourne null si le joueur n'est pas classé")
    void shouldReturnNullIfNotRanked() {
        when(zSetOps.reverseRank(anyString(), anyString())).thenReturn(null);

        Long rank = leaderboardService.getRank("unknown", "EASY");

        assertThat(rank).isNull();
    }

    private ZSetOperations.TypedTuple<String> mockTuple(String value, double score) {
        return new ZSetOperations.TypedTuple<>() {
            public String getValue() { return value; }
            public Double getScore() { return score; }
            public int compareTo(ZSetOperations.TypedTuple<String> o) { return Double.compare(score, o.getScore()); }
        };
    }
}
