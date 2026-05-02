package com.puzzlezen.game.service;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService — tests unitaires")
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    private List<Game> easyGames;

    @BeforeEach
    void setUp() {
        easyGames = IntStream.range(0, 5)
                .mapToObj(i -> Game.builder()
                        .id("id-" + i)
                        .title("Game " + i)
                        .type(Game.GameType.SUDOKU_4x4)
                        .difficulty(Game.Difficulty.EASY)
                        .build())
                .toList();
    }

    @Test
    @DisplayName("getRandomGamesForDifficulty retourne exactement 3 jeux")
    void shouldReturnExactlyThreeGames() {
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY)).thenReturn(easyGames);

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("getRandomGamesForDifficulty retourne des jeux distincts")
    void shouldReturnDistinctGames() {
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY)).thenReturn(easyGames);

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);

        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("getRandomGamesForDifficulty retourne tout si moins de 3 jeux disponibles")
    void shouldReturnAllWhenLessThanThreeAvailable() {
        List<Game> twoGames = easyGames.subList(0, 2);
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY)).thenReturn(twoGames);

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getRandomGamesForDifficulty retourne une liste vide si aucun jeu")
    void shouldReturnEmptyWhenNoGames() {
        when(gameRepository.findByDifficulty(Game.Difficulty.HARD)).thenReturn(List.of());

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.HARD);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("les appels successifs peuvent retourner des ordres différents")
    void shouldShuffleResults() {
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY)).thenReturn(easyGames);

        // On appelle plusieurs fois et on vérifie qu'au moins un ordre diffère
        boolean foundDifferent = false;
        List<Game> first = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);
        for (int i = 0; i < 20; i++) {
            List<Game> next = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);
            if (!first.stream().map(Game::getId).toList()
                    .equals(next.stream().map(Game::getId).toList())) {
                foundDifferent = true;
                break;
            }
        }
        // Probabilité d'échec ≈ (1/60)^20 ≈ 0
        assertThat(foundDifferent).isTrue();
    }
}
