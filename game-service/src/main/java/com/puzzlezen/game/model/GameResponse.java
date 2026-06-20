package com.puzzlezen.game.model;

import java.time.LocalDateTime;
import java.util.Map;

public record GameResponse(
        String id,
        String title,
        Game.GameType type,
        Game.Difficulty difficulty,
        Map<String, Object> config,
        LocalDateTime createdAt
) {}