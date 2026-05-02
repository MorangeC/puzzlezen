package com.puzzlezen.game.repository;

import com.puzzlezen.game.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameRepository extends MongoRepository<Game, String> {

    List<Game> findByDifficulty(Game.Difficulty difficulty);

    long countByDifficulty(Game.Difficulty difficulty);
}
