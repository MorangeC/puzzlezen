package com.puzzlezen.user.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game_results")
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String gameId;

    private String gameType;

    private String difficulty;

    private int score;

    /** Durée en secondes */
    private int durationSeconds;

    private boolean completed;

    @Builder.Default
    private LocalDateTime playedAt = LocalDateTime.now(ZoneId.of("UTC"));
}
