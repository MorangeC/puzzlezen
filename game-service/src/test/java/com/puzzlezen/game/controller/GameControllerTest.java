package com.puzzlezen.game.controller;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@DisplayName("GameController — tests d'intégration MockMvc")
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @Test
    @DisplayName("GET /api/games/session?difficulty=EASY retourne 200 et 3 jeux")
    void sessionEndpointReturnsThreeGames() throws Exception {
        List<Game> mockGames = List.of(
                Game.builder().id("1").title("Sudoku 4x4").type(Game.GameType.SUDOKU_4)
                        .difficulty(Game.Difficulty.EASY).config(Map.of("timeLimit", 300)).build(),
                Game.builder().id("2").title("Morse").type(Game.GameType.MORSE_DECODE)
                        .difficulty(Game.Difficulty.EASY).config(Map.of("timeLimit", 120)).build(),
                Game.builder().id("3").title("Puzzle").type(Game.GameType.PUZZLE_IMAGE)
                        .difficulty(Game.Difficulty.EASY).config(Map.of("timeLimit", 180)).build()
        );

        when(gameService.getRandomGamesForDifficulty(any())).thenReturn(mockGames);

        mockMvc.perform(get("/api/games/session").param("difficulty", "EASY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    @DisplayName("GET /api/games/session sans paramètre retourne 400")
    void sessionEndpointWithoutDifficultyReturns400() throws Exception {
        mockMvc.perform(get("/api/games/session"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/games/{id} inexistant retourne 404")
    void getUnknownGameReturns404() throws Exception {
        when(gameService.getById("unknown")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/games/unknown"))
                .andExpect(status().isNotFound());
    }
}
