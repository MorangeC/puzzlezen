package com.puzzlezen.game.model;


import java.util.Map;

public record GameRequest(
        String title,
        Game.GameType type,
        Game.Difficulty difficulty,
        Map<String, Object> config
) {}