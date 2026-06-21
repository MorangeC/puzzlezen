package com.puzzlezen.game.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record GameRequest(
        @NotBlank String title,
        @NotNull Game.GameType type,
        @NotNull Game.Difficulty difficulty,
        Map<String, Object> config
) {}