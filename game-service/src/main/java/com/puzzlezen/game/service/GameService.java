package com.puzzlezen.game.service;

import com.puzzlezen.game.model.Game;
import com.puzzlezen.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    /**
     * Retourne 3 jeux aléatoires distincts pour un niveau donné.
     * C'est la feature principale de l'appli.
     */
    public List<Game> getRandomGamesForDifficulty(Game.Difficulty difficulty) {

        List<Game> allGames = new ArrayList<>(
                gameRepository.findByDifficulty(difficulty)
        );

        if (allGames.size() < 3) {
            log.warn("Moins de 3 jeux disponibles pour le niveau {}, retour de tous les jeux disponibles", difficulty);
            return allGames;
        }

        // Mélange aléatoire + on prend les 3 premiers
        Collections.shuffle(allGames);
        return allGames.subList(0, 3);
    }

    public Optional<Game> getById(String id) {
        return gameRepository.findById(id);
    }

    public Game save(Game game) {
        game.setCreatedAt(java.time.LocalDateTime.now(ZoneId.of("UTC")));
        return gameRepository.save(game);
    }

    public List<Game> getAll() {
        return gameRepository.findAll();
    }
}
