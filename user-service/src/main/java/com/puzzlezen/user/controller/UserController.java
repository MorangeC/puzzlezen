package com.puzzlezen.user.controller;

import com.puzzlezen.user.model.GameResult;
import com.puzzlezen.user.model.UserProfile;
import com.puzzlezen.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "Profils joueurs, scores et historique des parties")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Profil joueur", description = "Score total, parties jouées/gagnées, dernière activité")
    @ApiResponse(responseCode = "200", description = "Profil retourné")
    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfile> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @Operation(summary = "Historique des parties", description = "Liste triée par date décroissante")
    @GetMapping("/{username}/history")
    public ResponseEntity<List<GameResult>> getHistory(@PathVariable String username) {
        return ResponseEntity.ok(userService.getHistory(username));
    }

    @Operation(summary = "Soumettre un résultat", description = "Enregistre le résultat d'une partie et met à jour les stats du joueur")
    @ApiResponse(responseCode = "200", description = "Résultat enregistré")
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
