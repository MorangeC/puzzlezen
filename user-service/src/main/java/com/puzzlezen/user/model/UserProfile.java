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
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Même username que dans auth-service — clé de liaison inter-services */
    @Column(unique = true, nullable = false)
    private String username;

    @Builder.Default
    private int totalScore = 0;

    @Builder.Default
    private int gamesPlayed = 0;

    @Builder.Default
    private int gamesWon = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("UTC"));

    private LocalDateTime lastPlayedAt;
}
