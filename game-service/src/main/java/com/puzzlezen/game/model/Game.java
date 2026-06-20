package com.puzzlezen.game.model;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Document(collection = "games")
public class Game {

    @Id
    private String id;

    private String title;

    private GameType type;

    private Difficulty difficulty;

    /** Configuration spécifique au type de jeu (grille sudoku, config parking...) */
    private Map<String, Object> config;

    private LocalDateTime createdAt;

    public enum GameType {
        SUDOKU_4,
        SUDOKU_9,
        PUZZLE_IMAGE,
        MORSE_DECODE,
        PARKING_RUSH,
        LABYRINTH,
        IMAGE_RECREATE,
        CAESAR_CIPHER,
        HANOI_TOWER
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}
