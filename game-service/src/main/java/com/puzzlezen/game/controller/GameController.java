package com.puzzlezen.game.controller;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * GET /api/games/session?difficulty=EASY
     * Retourne les 3 jeux aléatoires pour démarrer une session.
     */
    @GetMapping("/session")
    public ResponseEntity<List<Game>> getGameSession(
            @RequestParam Game.Difficulty difficulty) {
        List<Game> games = gameService.getRandomGamesForDifficulty(difficulty);
        return ResponseEntity.ok(games);
    }

    /**
     * GET /api/games/{id}
     * Détail d'un jeu spécifique (config, règles).
     */
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

    /**
     * POST /api/games
     * Ajouter un jeu à la banque (admin).
     */
    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody Game game) {
        return ResponseEntity.ok(gameService.save(game));
    }
}
