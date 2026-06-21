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
import java.util.Optional;
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
                        .type(Game.GameType.SUDOKU_4)
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
    void shouldAlwaysReturnMaxThreeGamesEvenIfMoreAvailable() {
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY))
                .thenReturn(easyGames);

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);

        assertThat(result)
                .hasSize(3)
                .allMatch(game -> game.getDifficulty() == Game.Difficulty.EASY);
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
    void shouldShuffleResultsDeterministically() {
        when(gameRepository.findByDifficulty(Game.Difficulty.EASY)).thenReturn(easyGames);

        List<Game> result = gameService.getRandomGamesForDifficulty(Game.Difficulty.EASY);

        assertThat(result)
                .hasSize(3)
                .extracting(Game::getId)
                .containsAnyOf("id-0", "id-1", "id-2", "id-3", "id-4");
    }

    @Test
    void shouldSetCreatedAtBeforeSaving() {
        Game input = Game.builder()
                .title("Test")
                .build();

        when(gameRepository.save(any(Game.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Game result = gameService.save(input);

        assertThat(result.getCreatedAt()).isNotNull();
        verify(gameRepository).save(input);
    }

    @Test
    void shouldReturnGameById() {
        Game game = Game.builder().id("123").build();

        when(gameRepository.findById("123"))
                .thenReturn(Optional.of(game));

        Optional<Game> result = gameService.getById("123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("123");
    }

    @Test
    void shouldReturnAllGames() {
        when(gameRepository.findAll()).thenReturn(easyGames);

        List<Game> result = gameService.getAll();

        assertThat(result).hasSize(5);
    }
}
