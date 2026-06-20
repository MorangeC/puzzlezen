package com.puzzlezen.game.controller;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.model.GameRequest;
import com.puzzlezen.game.model.GameResponse;
import com.puzzlezen.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Games", description = "Banque de jeux et sélection aléatoire pour les sessions")
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(
        summary = "Démarrer une session",
        description = "Retourne 3 jeux tirés aléatoirement parmi la banque pour le niveau choisi")
    @ApiResponse(responseCode = "200", description = "3 jeux retournés")
    @GetMapping("/session")
    public ResponseEntity<List<Game>> getGameSession(
            @Parameter(description = "Niveau de difficulté", example = "EASY")
            @RequestParam Game.Difficulty difficulty) {
        List<Game> games = gameService.getRandomGamesForDifficulty(difficulty);
        return ResponseEntity.ok(games);
    }

    @Operation(summary = "Détail d'un jeu", description = "Retourne la config complète d'un jeu (grille, solution, règles)")
    @ApiResponse(responseCode = "404", description = "Jeu introuvable")
    @GetMapping("/{id}")
    public ResponseEntity<Game> getGame(@PathVariable String id) {
        return gameService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/games
     * Liste tous les jeux (admin / debug).
     */
    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAll());
    }

    @Operation(summary = "Ajouter un jeu (admin)", description = "Ajoute un nouveau jeu à la banque MongoDB")
    @PostMapping
    public ResponseEntity<GameResponse> createGame(@RequestBody GameRequest request) {

        Game game = new Game();
        game.setTitle(request.title());
        game.setType(request.type());
        game.setDifficulty(request.difficulty());
        game.setConfig(request.config());
        game.setCreatedAt(LocalDateTime.now());

        Game saved = gameService.save(game);

        return ResponseEntity.ok(
                new GameResponse(
                        saved.getId(),
                        saved.getTitle(),
                        saved.getType(),
                        saved.getDifficulty(),
                        saved.getConfig(),
                        saved.getCreatedAt()
                )
        );
    }
}
