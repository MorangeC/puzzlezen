package com.puzzlezen.game.config;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Peuple la banque de jeux MongoDB au premier démarrage.
 * 3 jeux par niveau = 9 jeux au total.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameDataSeeder implements CommandLineRunner {

    private final GameRepository gameRepository;

    private static final String TIMELIMIT = "timeLimit";
    private static final String ORIENTATION = "orientation";
    private static final String LENGTH = "length";
    private static final String IS_TARGET = "isTarget";

    @Override
    public void run(String... args) {
        if (gameRepository.count() > 0) {
            log.info("Banque de jeux déjà peuplée ({} jeux), seed ignoré.", gameRepository.count());
            return;
        }

        log.info("Peuplement de la banque de jeux...");
        gameRepository.saveAll(buildGames());
        log.info("✅ {} jeux insérés.", gameRepository.count());
    }

    private List<Game> buildGames() {
        return List.of(

            // ─── FACILE ──────────────────────────────────────────
            Game.builder()
                .title("Sudoku 4×4")
                .type(Game.GameType.SUDOKU_4)
                .difficulty(Game.Difficulty.EASY)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "grid", List.of(
                        List.of(1, 0, 0, 4),
                        List.of(0, 4, 1, 0),
                        List.of(0, 1, 4, 0),
                        List.of(4, 0, 0, 1)
                    ),
                    "solution", List.of(
                        List.of(1, 2, 3, 4),
                        List.of(3, 4, 1, 2),
                        List.of(2, 1, 4, 3),
                        List.of(4, 3, 2, 1)
                    ),
                    TIMELIMIT, 300
                ))
                .build(),

            Game.builder()
                .title("Puzzle image — Paysage")
                .type(Game.GameType.PUZZLE_IMAGE)
                .difficulty(Game.Difficulty.EASY)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "imageUrl", "/assets/puzzles/landscape.jpg",
                    "pieces", 9,
                    "gridSize", "3x3",
                    TIMELIMIT, 180
                ))
                .build(),

            Game.builder()
                .title("Décode le morse")
                .type(Game.GameType.MORSE_DECODE)
                .difficulty(Game.Difficulty.EASY)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "message", ".... . .-.. .-.. ---",
                    "answer", "HELLO",
                    "hint", "5 lettres, un mot courant",
                    TIMELIMIT, 120
                ))
                .build(),

            // ─── INTERMÉDIAIRE ───────────────────────────────────
            Game.builder()
                .title("Parking Rush")
                .type(Game.GameType.PARKING_RUSH)
                .difficulty(Game.Difficulty.MEDIUM)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "gridSize", "6x6",
                    "exitCol", 5,
                    "exitRow", 2,
                    "cars", List.of(
                        Map.of("id","player","row",2,"col",0,LENGTH,2,ORIENTATION,"H",IS_TARGET,true),
                        Map.of("id","A","row",0,"col",2,LENGTH,2,ORIENTATION,"V",IS_TARGET,false),
                        Map.of("id","B","row",1,"col",3,LENGTH,3,ORIENTATION,"H",IS_TARGET,false),
                        Map.of("id","C","row",3,"col",1,LENGTH,2,ORIENTATION,"H",IS_TARGET,false)
                    ),
                    TIMELIMIT, 240
                ))
                .build(),

            Game.builder()
                .title("Labyrinthe")
                .type(Game.GameType.LABYRINTH)
                .difficulty(Game.Difficulty.MEDIUM)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "size", "10x10",
                    "start", Map.of("x",0,"y",0),
                    "end", Map.of("x",9,"y",9),
                    TIMELIMIT, 180
                ))
                .build(),

            Game.builder()
                .title("Recréer l'image")
                .type(Game.GameType.IMAGE_RECREATE)
                .difficulty(Game.Difficulty.MEDIUM)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "targetImage", "/assets/recreate/city.jpg",
                    "elements", 12,
                    TIMELIMIT, 300
                ))
                .build(),

            // ─── DIFFICILE ───────────────────────────────────────
            Game.builder()
                .title("Sudoku 9×9")
                .type(Game.GameType.SUDOKU_9)
                .difficulty(Game.Difficulty.HARD)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "difficulty", "expert",
                    "givenCells", 22,
                    TIMELIMIT, 1200
                ))
                .build(),

            Game.builder()
                .title("Chiffrement César")
                .type(Game.GameType.CAESAR_CIPHER)
                .difficulty(Game.Difficulty.HARD)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "encryptedMessage", "KHOOR ZRUOG",
                    "answer", "HELLO WORLD",
                    "shift", 3,
                    "hint", "César utilisait souvent le décalage 3",
                    TIMELIMIT, 180
                ))
                .build(),

            Game.builder()
                .title("Tour de Hanoï — 5 disques")
                .type(Game.GameType.HANOI_TOWER)
                .difficulty(Game.Difficulty.HARD)
                .createdAt(LocalDateTime.now())
                .config(Map.of(
                    "disks", 5,
                    "minMoves", 31,
                    TIMELIMIT, 600
                ))
                .build()
        );
    }
}
