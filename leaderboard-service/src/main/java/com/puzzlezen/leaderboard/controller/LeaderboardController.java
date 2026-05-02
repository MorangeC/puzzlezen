package com.puzzlezen.leaderboard.controller;

import com.puzzlezen.leaderboard.service.LeaderboardService;
import com.puzzlezen.leaderboard.service.LeaderboardService.LeaderboardEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Leaderboard", description = "Classements temps réel via Redis Sorted Sets — O(log n)")
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @Operation(summary = "Top global", description = "Classement toutes difficultés confondues")
    @GetMapping("/global")
    public ResponseEntity<List<LeaderboardEntry>> global(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalTop(limit));
    }

    @Operation(summary = "Top par niveau", description = "Classement pour EASY, MEDIUM ou HARD")
    @GetMapping("/{difficulty}")
    public ResponseEntity<List<LeaderboardEntry>> byDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getTop(difficulty.toUpperCase(), limit));
    }

    @Operation(summary = "Rang d'un joueur", description = "Position 1-based d'un joueur dans un classement donné")
    @GetMapping("/{difficulty}/rank/{username}")
    public ResponseEntity<?> getRank(
            @PathVariable String difficulty,
            @PathVariable String username) {
        Long rank = leaderboardService.getRank(username, difficulty.toUpperCase());
        if (rank == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(java.util.Map.of("username", username, "rank", rank));
    }
}
