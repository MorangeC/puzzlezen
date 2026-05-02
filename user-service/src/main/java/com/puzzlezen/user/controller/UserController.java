package com.puzzlezen.user.controller;

import com.puzzlezen.user.model.GameResult;
import com.puzzlezen.user.model.UserProfile;
import com.puzzlezen.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/{username}/profile */
    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(username));
    }

    /** GET /api/users/{username}/history */
    @GetMapping("/{username}/history")
    public ResponseEntity<List<GameResult>> getHistory(@PathVariable String username) {
        return ResponseEntity.ok(userService.getHistory(username));
    }

    /** POST /api/users/results — appelé par game-service après une partie */
    @PostMapping("/results")
    public ResponseEntity<GameResult> saveResult(@Valid @RequestBody GameResultRequest req) {
        GameResult result = userService.saveResult(
                req.getUsername(), req.getGameId(), req.getGameType(),
                req.getDifficulty(), req.getScore(), req.getDurationSeconds(), req.isCompleted()
        );
        return ResponseEntity.ok(result);
    }

    // ─── DTO ─────────────────────────────────────────────────────
    @Data
    public static class GameResultRequest {
        @NotBlank private String username;
        @NotBlank private String gameId;
        private String gameType;
        private String difficulty;
        private int score;
        private int durationSeconds;
        private boolean completed;
    }
}
