package com.puzzlezen.leaderboard.controller;

import com.puzzlezen.leaderboard.service.LeaderboardService;
import com.puzzlezen.leaderboard.service.LeaderboardService.LeaderboardEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /** GET /api/leaderboard/global?limit=10 */
    @GetMapping("/global")
    public ResponseEntity<List<LeaderboardEntry>> global(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalTop(limit));
    }

    /** GET /api/leaderboard/{difficulty}?limit=10 */
    @GetMapping("/{difficulty}")
    public ResponseEntity<List<LeaderboardEntry>> byDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getTop(difficulty.toUpperCase(), limit));
    }

    /** GET /api/leaderboard/{difficulty}/rank/{username} */
    @GetMapping("/{difficulty}/rank/{username}")
    public ResponseEntity<?> getRank(
            @PathVariable String difficulty,
            @PathVariable String username) {
        Long rank = leaderboardService.getRank(username, difficulty.toUpperCase());
        if (rank == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(java.util.Map.of("username", username, "rank", rank));
    }
}
